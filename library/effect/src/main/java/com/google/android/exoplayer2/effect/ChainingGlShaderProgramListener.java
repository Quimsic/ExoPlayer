/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.effect;

import android.util.Pair;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.effect.GlShaderProgram.InputListener;
import com.google.android.exoplayer2.effect.GlShaderProgram.OutputListener;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Connects a producing and a consuming {@link GlShaderProgram} instance.
 *
 * <p>This listener should be set as {@link InputListener} on the consuming {@link GlShaderProgram}
 * and as {@link OutputListener} on the producing {@link GlShaderProgram}.
 */
/* package */ final class ChainingGlShaderProgramListener
    implements GlShaderProgram.InputListener, GlShaderProgram.OutputListener {

  private final GlShaderProgram producingGlShaderProgram;
  private final GlShaderProgram consumingGlShaderProgram;
  private final FrameProcessingTaskExecutor frameProcessingTaskExecutor;

  @GuardedBy("this")
  private final Queue<Pair<TextureInfo, Long>> availableFrames;

  @GuardedBy("this")
  private int consumingGlShaderProgramInputCapacity;

  /**
   * Creates a new instance.
   *
   * @param producingGlShaderProgram The {@link GlShaderProgram} for which this listener will be set
   *     as {@link OutputListener}.
   * @param consumingGlShaderProgram The {@link GlShaderProgram} for which this listener will be set
   *     as {@link InputListener}.
   * @param frameProcessingTaskExecutor The {@link FrameProcessingTaskExecutor} that is used for
   *     OpenGL calls. All calls to the producing/consuming {@link GlShaderProgram} will be executed
   *     by the {@link FrameProcessingTaskExecutor}. The caller is responsible for releasing the
   *     {@link FrameProcessingTaskExecutor}.
   */
  public ChainingGlShaderProgramListener(
      GlShaderProgram producingGlShaderProgram,
      GlShaderProgram consumingGlShaderProgram,
      FrameProcessingTaskExecutor frameProcessingTaskExecutor) {
    this.producingGlShaderProgram = producingGlShaderProgram;
    this.consumingGlShaderProgram = consumingGlShaderProgram;
    this.frameProcessingTaskExecutor = frameProcessingTaskExecutor;
    availableFrames = new ArrayDeque<>();
  }

  @Override
  public synchronized void onReadyToAcceptInputFrame() {
    @Nullable Pair<TextureInfo, Long> pendingFrame = availableFrames.poll();
    if (pendingFrame == null) {
      consumingGlShaderProgramInputCapacity++;
      return;
    }

    long presentationTimeUs = pendingFrame.second;
    if (presentationTimeUs == C.TIME_END_OF_SOURCE) {
      frameProcessingTaskExecutor.submit(consumingGlShaderProgram::signalEndOfCurrentInputStream);
    } else {
      frameProcessingTaskExecutor.submit(
          () ->
              consumingGlShaderProgram.queueInputFrame(
                  /* inputTexture= */ pendingFrame.first, presentationTimeUs));
    }
  }

  @Override
  public void onInputFrameProcessed(TextureInfo inputTexture) {
    frameProcessingTaskExecutor.submit(
        () -> producingGlShaderProgram.releaseOutputFrame(inputTexture));
  }

  @Override
  public synchronized void onFlush() {
    consumingGlShaderProgramInputCapacity = 0;
    availableFrames.clear();
    frameProcessingTaskExecutor.submit(producingGlShaderProgram::flush);
  }

  @Override
  public synchronized void onOutputFrameAvailable(
      TextureInfo outputTexture, long presentationTimeUs) {
    if (consumingGlShaderProgramInputCapacity > 0) {
      frameProcessingTaskExecutor.submit(
          () ->
              consumingGlShaderProgram.queueInputFrame(
                  /* inputTexture= */ outputTexture, presentationTimeUs));
      consumingGlShaderProgramInputCapacity--;
    } else {
      availableFrames.add(new Pair<>(outputTexture, presentationTimeUs));
    }
  }

  @Override
  public synchronized void onCurrentOutputStreamEnded() {
    if (!availableFrames.isEmpty()) {
      availableFrames.add(new Pair<>(TextureInfo.UNSET, C.TIME_END_OF_SOURCE));
    } else {
      frameProcessingTaskExecutor.submit(consumingGlShaderProgram::signalEndOfCurrentInputStream);
    }
  }
}