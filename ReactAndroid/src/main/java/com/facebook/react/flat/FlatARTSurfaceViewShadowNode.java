/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.flat;

import javax.annotation.Nullable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.ReactShadowNode;
import com.facebook.react.uimanager.UIViewOperationQueue;
import com.facebook.react.views.art.ARTVirtualNode;
import com.facebook.yoga.YogaValue;
import com.facebook.yoga.YogaUnit;


/* package */ class FlatARTSurfaceViewShadowNode extends FlatShadowNode
    implements AndroidView, SurfaceHolder.Callback {
  private boolean mPaddingChanged = false;
  private @Nullable Surface mSurface;

  /* package */ FlatARTSurfaceViewShadowNode() {
    forceMountToView();
    forceMountChildrenToView();
  }

  @Override
  public boolean isVirtual() {
    return false;
  }

  @Override
  public boolean isVirtualAnchor() {
    return true;
  }

  @Override
  public void onCollectExtraUpdates(UIViewOperationQueue uiUpdater) {
    super.onCollectExtraUpdates(uiUpdater);
    drawOutput();
    uiUpdater.enqueueUpdateExtraData(getReactTag(), this);
  }

  private void drawOutput() {
    if (mSurface == null || !mSurface.isValid()) {
      markChildrenUpdatesSeen(this);
      return;
    }

    try {
      Canvas canvas = mSurface.lockCanvas(null);
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

      Paint paint = new Paint();
      for (int i = 0; i < getChildCount(); i++) {
        ARTVirtualNode child = (ARTVirtualNode) getChildAt(i);
        child.draw(canvas, paint, 1f);
        child.markUpdateSeen();
      }

      if (mSurface == null) {
        return;
      }

      mSurface.unlockCanvasAndPost(canvas);
    } catch (IllegalArgumentException | IllegalStateException e) {
      Log.e(ReactConstants.TAG, e.getClass().getSimpleName() + " in Surface.unlockCanvasAndPost");
    }
  }

  private void markChildrenUpdatesSeen(ReactShadowNode shadowNode) {
    for (int i = 0; i < shadowNode.getChildCount(); i++) {
      ReactShadowNode child = shadowNode.getChildAt(i);
      child.markUpdateSeen();
      markChildrenUpdatesSeen(child);
    }
  }

  @Override
  public boolean needsCustomLayoutForChildren() {
    return false;
  }

  @Override
  public boolean isPaddingChanged() {
    return mPaddingChanged;
  }

  @Override
  public void resetPaddingChanged() {
    mPaddingChanged = false;
  }

  @Override
  public void setPadding(int spacingType, float padding) {
    YogaValue current = getStylePadding(spacingType);
    if (current.unit != YogaUnit.POINT || current.value != padding) {
      super.setPadding(spacingType, padding);
      mPaddingChanged = true;
      markUpdated();
    }
  }

  @Override
  public void setPaddingPercent(int spacingType, float percent) {
    YogaValue current = getStylePadding(spacingType);
    if (current.unit != YogaUnit.PERCENT || current.value != percent) {
      super.setPadding(spacingType, percent);
      mPaddingChanged = true;
      markUpdated();
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    mSurface = holder.getSurface();
    drawOutput();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    mSurface = null;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
}
