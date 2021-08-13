package kr.co.anitex.golfteachingpro.videolistmanager

import kr.co.anitex.golfteachingpro.videolistmanager.DiskLruCache.Editor
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.*

/**
 *
 * Taken from the JB source code, can be found in:
 * libcore/luni/src/main/java/libcore/io/DiskLruCache.java
 * or direct link:
 * https://android.googlesource.com/platform/libcore/+/android-4.1.1_r1/luni/src/main/java/libcore/io/DiskLruCache.java
 *
 *
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Values are byte
 * sequences, accessible as streams or files. Each value must be between `0` and `Integer.MAX_VALUE` bytes in length.
 *
 *
 * The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 *
 *
 * This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 *
 *
 * Clients call [.edit] to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then [.edit] will return null.
 *
 *  * When an entry is being **created** it is necessary to
 * supply a full set of values; the empty value should be used as a
 * placeholder if necessary.
 *  * When an entry is being **edited**, it is not necessary
 * to supply data for every value; values default to their previous
 * value.
 *
 * Every [.edit] call must be matched by a call to [Editor.commit]
 * or [Editor.abort]. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 *
 *
 * Clients call [.get] to read a snapshot of an entry. The read will
 * observe the value at the time that [.get] was called. Updates and
 * removals after the call do not impact ongoing reads.
 *
 *
 * This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching `IOException` and
 * responding appropriately.
 */
class DiskLruCache private constructor(
    /**
     * Returns the directory where this cache stores its data.
     */
    /*
          * This cache uses a journal file named "journal". A typical journal file
          * looks like this:
          *     libcore.io.DiskLruCache
          *     1
          *     100
          *     2
          *
          *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
          *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
          *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
          *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
          *     DIRTY 1ab96a171faeeee38496d8b330771a7a
          *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
          *     READ 335c4c6028171cfddfbaae1a9c313c52
          *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
          *
          * The first five lines of the journal form its header. They are the
          * constant string "libcore.io.DiskLruCache", the disk cache's version,
          * the application's version, the value count, and a blank line.
          *
          * Each of the subsequent lines in the file is a record of the state of a
          * cache entry. Each line contains space-separated values: a state, a key,
          * and optional state-specific values.
          *   o DIRTY lines track that an entry is actively being created or updated.
          *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
          *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
          *     temporary files may need to be deleted.
          *   o CLEAN lines track a cache entry that has been successfully published
          *     and may be read. A publish line is followed by the lengths of each of
          *     its values.
          *   o READ lines track accesses for LRU.
          *   o REMOVE lines track entries that have been deleted.
          *
          * The journal file is appended to as cache operations occur. The journal may
          * occasionally be compacted by dropping redundant lines. A temporary file named
          * "journal.tmp" will be used during compaction; that file should be deleted if
          * it exists when the cache is opened.
          */
    val directory: File, private val appVersion: Int, valueCount: Int, maxSize: Long
) :
    Closeable {
    private val journalFile: File
    private val journalFileTmp: File
    private val maxSize: Long
    private val valueCount: Int
    private var size: Long = 0
    private var journalWriter: Writer? = null
    private val lruEntries = LinkedHashMap<String, Entry?>(0, 0.75f, true)
    private var redundantOpCount = 0

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private var nextSequenceNumber: Long = 0

    /** This cache uses a single background thread to evict entries.  */
    private val executorService: ExecutorService = ThreadPoolExecutor(
        0, 1,
        60L, TimeUnit.SECONDS, LinkedBlockingQueue()
    )
    private val cleanupCallable: Callable<Void> = Callable {
        synchronized(this@DiskLruCache) {
            if (journalWriter == null) {
                return@Callable null // closed
            }
            trimToSize()
            if (journalRebuildRequired()) {
                rebuildJournal()
                redundantOpCount = 0
            }
        }
        null
    }

    @Throws(IOException::class)
    private fun readJournal() {
        val `in`: InputStream = BufferedInputStream(FileInputStream(journalFile), IO_BUFFER_SIZE)
        try {
            val magic = readAsciiLine(`in`)
            val version = readAsciiLine(`in`)
            val appVersionString = readAsciiLine(`in`)
            val valueCountString = readAsciiLine(`in`)
            val blank = readAsciiLine(`in`)
            if (MAGIC != magic
                || VERSION_1 != version
                || Integer.toString(appVersion) != appVersionString
                || Integer.toString(valueCount) != valueCountString
                || "" != blank
            ) {
                throw IOException(
                    "unexpected journal header: ["
                            + magic + ", " + version + ", " + valueCountString + ", " + blank + "]"
                )
            }
            while (true) {
                try {
                    readJournalLine(readAsciiLine(`in`))
                } catch (endOfJournal: EOFException) {
                    break
                }
            }
        } finally {
            closeQuietly(`in`)
        }
    }

    @Throws(IOException::class)
    private fun readJournalLine(line: String) {
        val parts = line.split(" ".toRegex()).toTypedArray()
        if (parts.size < 2) {
            throw IOException("unexpected journal line: $line")
        }
        val key = parts[1]
        if ((parts[0] == REMOVE) && parts.size == 2) {
            lruEntries.remove(key)
            return
        }
        var entry = lruEntries[key]
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        }
        if ((parts[0] == CLEAN) && parts.size == 2 + valueCount) {
            entry.readable = true
            entry.currentEditor = null
            entry.setLengths(copyOfRange(parts, 2, parts.size))
        } else if ((parts[0] == DIRTY) && parts.size == 2) {
            entry.currentEditor = Editor(entry)
        } else if ((parts[0] == READ) && parts.size == 2) {
            // this work was already done by calling lruEntries.get()
        } else {
            throw IOException("unexpected journal line: $line")
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the
     * cache. Dirty entries are assumed to be inconsistent and will be deleted.
     */
    @Throws(IOException::class)
    private fun processJournal() {
        deleteIfExists(journalFileTmp)
        val i = lruEntries.values.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry!!.currentEditor == null) {
                for (t in 0 until valueCount) {
                    size += entry.lengths[t]
                }
            } else {
                entry.currentEditor = null
                for (t in 0 until valueCount) {
                    deleteIfExists(entry.getCleanFile(t))
                    deleteIfExists(entry.getDirtyFile(t))
                }
                i.remove()
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the
     * current journal if it exists.
     */
    @Synchronized
    @Throws(IOException::class)
    private fun rebuildJournal() {
        if (journalWriter != null) {
            journalWriter!!.close()
        }
        val writer: Writer = BufferedWriter(FileWriter(journalFileTmp), IO_BUFFER_SIZE)
        writer.write(MAGIC)
        writer.write("\n")
        writer.write(VERSION_1)
        writer.write("\n")
        writer.write(Integer.toString(appVersion))
        writer.write("\n")
        writer.write(Integer.toString(valueCount))
        writer.write("\n")
        writer.write("\n")
        for (entry: Entry? in lruEntries.values) {
            if (entry!!.currentEditor != null) {
                writer.write(DIRTY + ' ' + entry.key + '\n')
            } else {
                writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n')
            }
        }
        writer.close()
        journalFileTmp.renameTo(journalFile)
        journalWriter = BufferedWriter(FileWriter(journalFile, true), IO_BUFFER_SIZE)
    }

    /**
     * Returns a snapshot of the entry named `key`, or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     */
    @Synchronized
    @Throws(IOException::class)
    operator fun get(key: String): Snapshot? {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries.get(key) ?: return null
        if (!entry.readable) {
            return null
        }

        /*
         * Open all streams eagerly to guarantee that we see a single published
         * snapshot. If we opened streams lazily then the streams could come
         * from different edits.
         */
        val ins = arrayOfNulls<InputStream>(valueCount)
        try {
            for (i in 0 until valueCount) {
                ins[i] = FileInputStream(entry.getCleanFile(i))
            }
        } catch (e: FileNotFoundException) {
            // a file must have been deleted manually!
            return null
        }
        redundantOpCount++
        journalWriter!!.append(READ + ' ' + key + '\n')
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return Snapshot(key, entry.sequenceNumber, ins)
    }

    /**
     * Returns an editor for the entry named `key`, or null if another
     * edit is in progress.
     */
    @Throws(IOException::class)
    fun edit(key: String): Editor? {
        return edit(key, ANY_SEQUENCE_NUMBER)
    }

    @Synchronized
    @Throws(IOException::class)
    private fun edit(key: String, expectedSequenceNumber: Long): Editor? {
        checkNotClosed()
        validateKey(key)
        var entry = lruEntries[key]
        if ((expectedSequenceNumber != ANY_SEQUENCE_NUMBER
                    && (entry == null || entry.sequenceNumber != expectedSequenceNumber))
        ) {
            return null // snapshot is stale
        }
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        } else if (entry.currentEditor != null) {
            return null // another edit is in progress
        }
        val editor = Editor(entry)
        entry.currentEditor = editor

        // flush the journal before creating files to prevent file leaks
        journalWriter!!.write(DIRTY + ' ' + key + '\n')
        journalWriter!!.flush()
        return editor
    }

    /**
     * Returns the maximum number of bytes that this cache should use to store
     * its data.
     */
    fun maxSize(): Long {
        return maxSize
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    @Synchronized
    fun size(): Long {
        return size
    }

    @Synchronized
    @Throws(IOException::class)
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        if (entry.currentEditor != editor) {
            throw IllegalStateException()
        }

        // if this edit is creating the entry for the first time, every index must have a value
        if (success && !entry.readable) {
            for (i in 0 until valueCount) {
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort()
                    throw IllegalStateException("edit didn't create file $i")
                }
            }
        }
        for (i in 0 until valueCount) {
            val dirty = entry.getDirtyFile(i)
            if (success) {
                if (dirty.exists()) {
                    val clean = entry.getCleanFile(i)
                    dirty.renameTo(clean)
                    val oldLength = entry.lengths[i]
                    val newLength = clean.length()
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                }
            } else {
                deleteIfExists(dirty)
            }
        }
        redundantOpCount++
        entry.currentEditor = null
        if (entry.readable or success) {
            entry.readable = true
            journalWriter!!.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n')
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++
            }
        } else {
            lruEntries.remove(entry.key)
            journalWriter!!.write(REMOVE + ' ' + entry.key + '\n')
        }
        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal
     * and eliminate at least 2000 ops.
     */
    private fun journalRebuildRequired(): Boolean {
        val REDUNDANT_OP_COMPACT_THRESHOLD = 2000
        return (redundantOpCount >= REDUNDANT_OP_COMPACT_THRESHOLD
                && redundantOpCount >= lruEntries.size)
    }

    /**
     * Drops the entry for `key` if it exists and can be removed. Entries
     * actively being edited cannot be removed.
     *
     * @return true if an entry was removed.
     */
    @Synchronized
    @Throws(IOException::class)
    fun remove(key: String): Boolean {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key]
        if (entry == null || entry.currentEditor != null) {
            return false
        }
        for (i in 0 until valueCount) {
            val file = entry.getCleanFile(i)
            if (!file.delete()) {
                throw IOException("failed to delete $file")
            }
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }
        redundantOpCount++
        journalWriter!!.append(REMOVE + ' ' + key + '\n')
        lruEntries.remove(key)
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return true
    }

    /**
     * Returns true if this cache has been closed.
     */
    val isClosed: Boolean
        get() = journalWriter == null

    private fun checkNotClosed() {
        if (journalWriter == null) {
            throw IllegalStateException("cache is closed")
        }
    }

    /**
     * Force buffered operations to the filesystem.
     */
    @Synchronized
    @Throws(IOException::class)
    fun flush() {
        checkNotClosed()
        trimToSize()
        journalWriter!!.flush()
    }

    /**
     * Closes this cache. Stored values will remain on the filesystem.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun close() {
        if (journalWriter == null) {
            return  // already closed
        }
        for (entry: Entry? in ArrayList(lruEntries.values)) {
            if (entry!!.currentEditor != null) {
                entry.currentEditor?.abort()
            }
        }
        trimToSize()
        journalWriter!!.close()
        journalWriter = null
    }

    @Throws(IOException::class)
    private fun trimToSize() {
        while (size > maxSize) {
//            Map.Entry<String, Entry> toEvict = lruEntries.eldest();
            val toEvict: Map.Entry<String, Entry?> = lruEntries.entries.iterator().next()
            remove(toEvict.key)
        }
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete
     * all files in the cache directory including files that weren't created by
     * the cache.
     */
    @Throws(IOException::class)
    fun delete() {
        close()
        deleteContents(directory)
    }

    private fun validateKey(key: String) {
        if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
            throw IllegalArgumentException(
                "keys must not contain spaces or newlines: \"$key\""
            )
        }
    }

    /**
     * A snapshot of the values for an entry.
     */
    inner class Snapshot(
        private val key: String,
        private val sequenceNumber: Long,
        private val ins: Array<InputStream?>
    ) :
        Closeable {
        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress.
         */
        @Throws(IOException::class)
        fun edit(): Editor? {
            return this@DiskLruCache.edit(key, sequenceNumber)
        }

        /**
         * Returns the unbuffered stream with the value for `index`.
         */
        fun getInputStream(index: Int): InputStream? {
            return ins[index]
        }

        /**
         * Returns the string value for `index`.
         */
        @Throws(IOException::class)
        fun getString(index: Int): String {
            return inputStreamToString(getInputStream(index))
        }

        override fun close() {
            for (`in`: InputStream? in ins) {
                closeQuietly(`in`)
            }
        }
    }

    /**
     * Edits the values for an entry.
     */
    inner class Editor(val entry: Entry) {
        private var hasErrors = false

        /**
         * Returns an unbuffered input stream to read the last committed value,
         * or null if no value has been committed.
         */
        @Throws(IOException::class)
        fun newInputStream(index: Int): InputStream? {
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalStateException()
                }
                if (!entry.readable) {
                    return null
                }
                return FileInputStream(entry.getCleanFile(index))
            }
        }

        /**
         * Returns the last committed value as a string, or null if no value
         * has been committed.
         */
        @Throws(IOException::class)
        fun getString(index: Int): String? {
            val `in` = newInputStream(index)
            return if (`in` != null) inputStreamToString(`in`) else null
        }

        /**
         * Returns a new unbuffered output stream to write the value at
         * `index`. If the underlying output stream encounters errors
         * when writing to the filesystem, this edit will be aborted when
         * [.commit] is called. The returned output stream does not throw
         * IOExceptions.
         */
        @Throws(IOException::class)
        fun newOutputStream(index: Int): OutputStream {
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalStateException()
                }
                return FaultHidingOutputStream(
                    FileOutputStream(
                        entry.getDirtyFile(
                            index
                        )
                    )
                )
            }
        }

        /**
         * Sets the value at `index` to `value`.
         */
        @Throws(IOException::class)
        operator fun set(index: Int, value: String?) {
            var writer: Writer? = null
            try {
                writer = OutputStreamWriter(newOutputStream(index), UTF_8)
                writer.write(value)
            } finally {
                closeQuietly(writer)
            }
        }

        /**
         * Commits this edit so it is visible to readers.  This releases the
         * edit lock so another edit may be started on the same key.
         */
        @Throws(IOException::class)
        fun commit() {
            if (hasErrors) {
                completeEdit(this, false)
                remove(entry.key) // the previous entry is stale
            } else {
                completeEdit(this, true)
            }
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         */
        @Throws(IOException::class)
        fun abort() {
            completeEdit(this, false)
        }

        private inner class FaultHidingOutputStream(out: OutputStream) :
            FilterOutputStream(out) {
            override fun write(oneByte: Int) {
                try {
                    out.write(oneByte)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                try {
                    out.write(buffer, offset, length)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun close() {
                try {
                    out.close()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun flush() {
                try {
                    out.flush()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }
        }
    }

    inner class Entry(val key: String) {
        /** Lengths of this entry's files.  */
        val lengths: LongArray = LongArray(valueCount)

        /** True if this entry has ever been published  */
        var readable = false

        /** The ongoing edit or null if this entry is not being edited.  */
        var currentEditor: Editor? = null

        /** The sequence number of the most recently committed edit to this entry.  */
        var sequenceNumber: Long = 0
        @Throws(IOException::class)
        fun getLengths(): String {
            val result = StringBuilder()
            for (size: Long in lengths) {
                result.append(' ').append(size)
            }
            return result.toString()
        }

        /**
         * Set lengths using decimal numbers like "10123".
         */
        @Throws(IOException::class)
        fun setLengths(strings: Array<String>) {
            if (strings.size != valueCount) {
                throw invalidLengths(strings)
            }
            try {
                for (i in strings.indices) {
                    lengths[i] = strings[i].toLong()
                }
            } catch (e: NumberFormatException) {
                throw invalidLengths(strings)
            }
        }

        @Throws(IOException::class)
        private fun invalidLengths(strings: Array<String>): IOException {
            throw IOException("unexpected journal line: " + Arrays.toString(strings))
        }

        fun getCleanFile(i: Int): File {
            return File(directory, "$key.$i")
        }

        fun getDirtyFile(i: Int): File {
            return File(directory, "$key.$i.tmp")
        }

    }

    companion object {
        val JOURNAL_FILE = "journal"
        val JOURNAL_FILE_TMP = "journal.tmp"
        val MAGIC = "libcore.io.DiskLruCache"
        val VERSION_1 = "1"
        val ANY_SEQUENCE_NUMBER: Long = -1
        private val CLEAN = "CLEAN"
        private val DIRTY = "DIRTY"
        private val REMOVE = "REMOVE"
        private val READ = "READ"
        private val UTF_8 = Charset.forName("UTF-8")
        private val IO_BUFFER_SIZE = 8 * 1024

        /* From java.util.Arrays */
        private fun <T> copyOfRange(original: Array<T>, start: Int, end: Int): Array<T> {
            val originalLength = original.size // For exception priority compatibility.
            if (start > end) {
                throw IllegalArgumentException()
            }
            if (start < 0 || start > originalLength) {
                throw ArrayIndexOutOfBoundsException()
            }
            val resultLength = end - start
            val copyLength = Math.min(resultLength, originalLength - start)
            val result = java.lang.reflect.Array
                .newInstance(original.javaClass.componentType, resultLength) as Array<T>
            System.arraycopy(original, start, result, 0, copyLength)
            return result
        }

        /**
         * Returns the remainder of 'reader' as a string, closing it when done.
         */
        @Throws(IOException::class)
        fun readFully(reader: Reader): String {
            try {
                val writer = StringWriter()
                val buffer = CharArray(1024)
                var count: Int
                while ((reader.read(buffer).also { count = it }) != -1) {
                    writer.write(buffer, 0, count)
                }
                return writer.toString()
            } finally {
                reader.close()
            }
        }

        /**
         * Returns the ASCII characters up to but not including the next "\r\n", or
         * "\n".
         *
         * @throws EOFException if the stream is exhausted before the next newline
         * character.
         */
        @Throws(IOException::class)
        fun readAsciiLine(`in`: InputStream): String {
            // TODO: support UTF-8 here instead
            val result = StringBuilder(80)
            while (true) {
                val c = `in`.read()
                if (c == -1) {
                    throw EOFException()
                } else if (c == '\n'.toInt()) {
                    break
                }
                result.append(c.toChar())
            }
            val length = result.length
            if (length > 0 && result[length - 1] == '\r') {
                result.setLength(length - 1)
            }
            return result.toString()
        }

        /**
         * Closes 'closeable', ignoring any checked exceptions. Does nothing if 'closeable' is null.
         */
        fun closeQuietly(closeable: Closeable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (rethrown: RuntimeException) {
                    throw rethrown
                } catch (ignored: Exception) {
                }
            }
        }

        /**
         * Recursively delete everything in `dir`.
         */
        // TODO: this should specify paths as Strings rather than as Files
        @Throws(IOException::class)
        fun deleteContents(dir: File) {
            val files = dir.listFiles() ?: throw IllegalArgumentException("not a directory: $dir")
            for (file: File in files) {
                if (file.isDirectory) {
                    deleteContents(file)
                }
                if (!file.delete()) {
                    throw IOException("failed to delete file: $file")
                }
            }
        }

        /**
         * Opens the cache in `directory`, creating a cache if none exists
         * there.
         *
         * @param directory a writable directory
         * @param appVersion
         * @param valueCount the number of values per cache entry. Must be positive.
         * @param maxSize the maximum number of bytes this cache should use to store
         * @throws IOException if reading or writing the cache directory fails
         */
        @Throws(IOException::class)
        fun open(directory: File, appVersion: Int, valueCount: Int, maxSize: Long): DiskLruCache {
            if (maxSize <= 0) {
                throw IllegalArgumentException("maxSize <= 0")
            }
            if (valueCount <= 0) {
                throw IllegalArgumentException("valueCount <= 0")
            }

            // prefer to pick up where we left off
            var cache = DiskLruCache(directory, appVersion, valueCount, maxSize)
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal()
                    cache.processJournal()
                    cache.journalWriter = BufferedWriter(
                        FileWriter(cache.journalFile, true),
                        IO_BUFFER_SIZE
                    )
                    return cache
                } catch (journalIsCorrupt: IOException) {
//                System.logW("DiskLruCache " + directory + " is corrupt: "
//                        + journalIsCorrupt.getMessage() + ", removing");
                    cache.delete()
                }
            }

            // create a new empty cache
            directory.mkdirs()
            cache = DiskLruCache(directory, appVersion, valueCount, maxSize)
            cache.rebuildJournal()
            return cache
        }

        @Throws(IOException::class)
        private fun deleteIfExists(file: File) {
//        try {
//            Libcore.os.remove(file.getPath());
//        } catch (ErrnoException errnoException) {
//            if (errnoException.errno != OsConstants.ENOENT) {
//                throw errnoException.rethrowAsIOException();
//            }
//        }
            if (file.exists() && !file.delete()) {
                throw IOException()
            }
        }

        @Throws(IOException::class)
        private fun inputStreamToString(`in`: InputStream?): String {
            return readFully(InputStreamReader(`in`, UTF_8))
        }
    }

    init {
        journalFile = File(directory, JOURNAL_FILE)
        journalFileTmp = File(directory, JOURNAL_FILE_TMP)
        this.valueCount = valueCount
        this.maxSize = maxSize
    }
}
