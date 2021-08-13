package com.mocircle.cidrawing.utils;

import com.mocircle.cidrawing.element.DrawElement;

import java.util.List;

public class ElementUtils {

    public static void sortElementsInLayer(List<DrawElement> elements) {
        elements.sort((o1, o2) -> o1.getOrderIndex() - o2.getOrderIndex());
    }

}
