/*
 * Copyright 2021 The Android Open Source Project
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
package com.google.android.exoplayer2.transformer.mh.analysis;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.effect.ScaleAndRotateTransformation;
import com.google.android.exoplayer2.transformer.AndroidTestUtil;
import com.google.android.exoplayer2.transformer.EditedMediaItem;
import com.google.android.exoplayer2.transformer.Effects;
import com.google.android.exoplayer2.transformer.ExportTestResult;
import com.google.android.exoplayer2.transformer.TransformationRequest;
import com.google.android.exoplayer2.transformer.Transformer;
import com.google.android.exoplayer2.transformer.TransformerAndroidTestRunner;
import com.google.android.exoplayer2.util.Effect;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests repeated transcoding operations (as a stress test and to help reproduce flakiness). */
@RunWith(AndroidJUnit4.class)
public final class RepeatedTranscodeTest {
  private static final int TRANSCODE_COUNT = 10;

  @Test
  public void repeatedTranscode_givesConsistentLengthOutput() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();

    TransformerAndroidTestRunner transformerRunner =
        new TransformerAndroidTestRunner.Builder(
                context,
                new Transformer.Builder(context)
                    .setEncoderFactory(new AndroidTestUtil.ForceEncodeEncoderFactory(context))
                    .build())
            .build();
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse(AndroidTestUtil.MP4_REMOTE_10_SECONDS_URI_STRING));
    ImmutableList<Effect> videoEffects =
        ImmutableList.of(new ScaleAndRotateTransformation.Builder().setRotationDegrees(45).build());
    Effects effects = new Effects(/* audioProcessors= */ ImmutableList.of(), videoEffects);
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(effects).build();

    Set<Long> differentOutputSizesBytes = new HashSet<>();
    for (int i = 0; i < TRANSCODE_COUNT; i++) {
      // Use a long video in case an error occurs a while after the start of the video.
      ExportTestResult testResult =
          transformerRunner.run(
              /* testId= */ "repeatedTranscode_givesConsistentLengthOutput_" + i, editedMediaItem);
      differentOutputSizesBytes.add(checkNotNull(testResult.exportResult.fileSizeBytes));
    }

    assertWithMessage(
            "Different transcoding output sizes detected. Sizes: " + differentOutputSizesBytes)
        .that(differentOutputSizesBytes.size())
        .isEqualTo(1);
  }

  @Test
  public void repeatedTranscodeNoAudio_givesConsistentLengthOutput() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    TransformerAndroidTestRunner transformerRunner =
        new TransformerAndroidTestRunner.Builder(
                context,
                new Transformer.Builder(context)
                    .setEncoderFactory(new AndroidTestUtil.ForceEncodeEncoderFactory(context))
                    .build())
            .build();
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse(AndroidTestUtil.MP4_REMOTE_10_SECONDS_URI_STRING));
    ImmutableList<Effect> videoEffects =
        ImmutableList.of(new ScaleAndRotateTransformation.Builder().setRotationDegrees(45).build());
    Effects effects = new Effects(/* audioProcessors= */ ImmutableList.of(), videoEffects);
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setRemoveAudio(true).setEffects(effects).build();

    Set<Long> differentOutputSizesBytes = new HashSet<>();
    for (int i = 0; i < TRANSCODE_COUNT; i++) {
      // Use a long video in case an error occurs a while after the start of the video.
      ExportTestResult testResult =
          transformerRunner.run(
              /* testId= */ "repeatedTranscodeNoAudio_givesConsistentLengthOutput_" + i,
              editedMediaItem);
      differentOutputSizesBytes.add(checkNotNull(testResult.exportResult.fileSizeBytes));
    }

    assertWithMessage(
            "Different transcoding output sizes detected. Sizes: " + differentOutputSizesBytes)
        .that(differentOutputSizesBytes.size())
        .isEqualTo(1);
  }

  @Test
  public void repeatedTranscodeNoVideo_givesConsistentLengthOutput() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    TransformerAndroidTestRunner transformerRunner =
        new TransformerAndroidTestRunner.Builder(
                context,
                new Transformer.Builder(context)
                    .setTransformationRequest(new TransformationRequest.Builder().build())
                    .setEncoderFactory(new AndroidTestUtil.ForceEncodeEncoderFactory(context))
                    .build())
            .build();
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse(AndroidTestUtil.MP4_REMOTE_10_SECONDS_URI_STRING));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setRemoveVideo(true).build();

    Set<Long> differentOutputSizesBytes = new HashSet<>();
    for (int i = 0; i < TRANSCODE_COUNT; i++) {
      // Use a long video in case an error occurs a while after the start of the video.
      ExportTestResult testResult =
          transformerRunner.run(
              /* testId= */ "repeatedTranscodeNoVideo_givesConsistentLengthOutput_" + i,
              editedMediaItem);
      differentOutputSizesBytes.add(checkNotNull(testResult.exportResult.fileSizeBytes));
    }

    assertWithMessage(
            "Different transcoding output sizes detected. Sizes: " + differentOutputSizesBytes)
        .that(differentOutputSizesBytes.size())
        .isEqualTo(1);
  }
}
