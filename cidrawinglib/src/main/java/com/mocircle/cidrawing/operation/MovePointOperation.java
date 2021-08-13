package com.mocircle.cidrawing.operation;

import android.graphics.PointF;

public class MovePointOperation extends AbstractOperation {

    private final PointF point;
    private final float deltaX;
    private final float deltaY;

    public MovePointOperation(PointF point, float deltaX, float deltaY) {
        this.point = point;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override
    public boolean isExecutable() {
        return point != null;
    }

    @Override
    public boolean doOperation() {
        point.offset(deltaX, deltaY);
        drawingBoard.getDrawingView().notifyViewUpdated();
        return true;
    }

    @Override
    public void undo() {
        point.offset(-deltaX, -deltaY);
        drawingBoard.getDrawingView().notifyViewUpdated();
    }
}
