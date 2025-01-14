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
package com.google.android.exoplayer2.transformer.mh;

import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_1080P_1_SECOND_HDR10_VIDEO_SDR_CONTAINER;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_1080P_5_SECOND_HLG10;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_720P_4_SECOND_HDR10;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.recordTestSkipped;
import static com.google.android.exoplayer2.transformer.mh.FileUtil.maybeAssertFileHasColorTransfer;
import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.android.exoplayer2.util.MimeTypes.VIDEO_H265;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.effect.ScaleAndRotateTransformation;
import com.google.android.exoplayer2.transformer.EditedMediaItem;
import com.google.android.exoplayer2.transformer.Effects;
import com.google.android.exoplayer2.transformer.EncoderUtil;
import com.google.android.exoplayer2.transformer.ExportException;
import com.google.android.exoplayer2.transformer.ExportTestResult;
import com.google.android.exoplayer2.transformer.TransformationRequest;
import com.google.android.exoplayer2.transformer.Transformer;
import com.google.android.exoplayer2.transformer.TransformerAndroidTestRunner;
import com.google.android.exoplayer2.util.Effect;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.video.ColorInfo;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link Transformer} instrumentation test for applying an {@linkplain
 * TransformationRequest#HDR_MODE_KEEP_HDR HDR frame edit}.
 */
@RunWith(AndroidJUnit4.class)
public class HdrEditingTest {
  public static final String TAG = "HdrEditingTest";
  private static final ColorInfo HDR10_DEFAULT_COLOR_INFO =
      new ColorInfo.Builder()
          .setColorSpace(C.COLOR_SPACE_BT2020)
          .setColorRange(C.COLOR_RANGE_LIMITED)
          .setColorTransfer(C.COLOR_TRANSFER_ST2084)
          .build();
  private static final ColorInfo HLG10_DEFAULT_COLOR_INFO =
      new ColorInfo.Builder()
          .setColorSpace(C.COLOR_SPACE_BT2020)
          .setColorRange(C.COLOR_RANGE_LIMITED)
          .setColorTransfer(C.COLOR_TRANSFER_HLG)
          .build();

  @Test
  public void export_noRequestedTranscode_hdr10File_exportsOrThrows() throws Exception {
    String testId = "export_noRequestedTranscode_hdr10File_exportsOrThrows";
    Context context = ApplicationProvider.getApplicationContext();

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_720P_4_SECOND_HDR10));

    try {
      ExportTestResult exportTestResult =
          new TransformerAndroidTestRunner.Builder(context, transformer)
              .build()
              .run(testId, mediaItem);
      Log.i(TAG, "Exported.");
      maybeAssertFileHasColorTransfer(exportTestResult.filePath, C.COLOR_TRANSFER_ST2084);
    } catch (ExportException exception) {
      Log.i(TAG, checkNotNull(exception.getCause()).toString());
      assertThat(exception).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
      assertThat(exception.errorCode)
          .isAnyOf(
              ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
              ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED);
    }
  }

  @Test
  public void export_noRequestedTranscode_hlg10File_exportsOrThrows() throws Exception {
    String testId = "export_noRequestedTranscode_hlg10File_exportsOrThrows";
    Context context = ApplicationProvider.getApplicationContext();

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_5_SECOND_HLG10));

    try {
      ExportTestResult exportTestResult =
          new TransformerAndroidTestRunner.Builder(context, transformer)
              .build()
              .run(testId, mediaItem);
      Log.i(TAG, "Exported.");
      maybeAssertFileHasColorTransfer(exportTestResult.filePath, C.COLOR_TRANSFER_HLG);
    } catch (ExportException exception) {
      Log.i(TAG, checkNotNull(exception.getCause()).toString());
      assertThat(exception).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
      assertThat(exception.errorCode)
          .isAnyOf(
              ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
              ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED);
    }
  }

  @Test
  public void exportAndTranscode_hdr10File_whenHdrEditingIsSupported_exports() throws Exception {
    String testId = "exportAndTranscode_hdr10File_whenHdrEditingIsSupported_exports";
    Context context = ApplicationProvider.getApplicationContext();
    if (!deviceSupportsHdrEditing(VIDEO_H265, HDR10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device lacks HDR10 editing support.");
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_720P_4_SECOND_HDR10));
    ImmutableList<Effect> videoEffects =
        ImmutableList.of(
            new ScaleAndRotateTransformation.Builder().setRotationDegrees(180).build());
    Effects effects = new Effects(/* audioProcessors= */ ImmutableList.of(), videoEffects);
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(effects).build();

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, editedMediaItem);
    maybeAssertFileHasColorTransfer(exportTestResult.filePath, C.COLOR_TRANSFER_ST2084);
  }

  @Test
  public void exportAndTranscode_hlg10File_whenHdrEditingIsSupported_exports() throws Exception {
    String testId = "exportAndTranscode_hlg10File_whenHdrEditingIsSupported_exports";
    Context context = ApplicationProvider.getApplicationContext();
    if (!deviceSupportsHdrEditing(VIDEO_H265, HLG10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device lacks HLG10 editing support.");
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_5_SECOND_HLG10));
    ImmutableList<Effect> videoEffects =
        ImmutableList.of(
            new ScaleAndRotateTransformation.Builder().setRotationDegrees(180).build());
    Effects effects = new Effects(/* audioProcessors= */ ImmutableList.of(), videoEffects);
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(effects).build();

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, editedMediaItem);
    maybeAssertFileHasColorTransfer(exportTestResult.filePath, C.COLOR_TRANSFER_HLG);
  }

  @Test
  public void exportAndTranscode_hdr10File_whenHdrEditingUnsupported_toneMapsOrThrows()
      throws Exception {
    String testId = "exportAndTranscode_hdr10File_whenHdrEditingUnsupported_toneMapsOrThrows";
    Context context = ApplicationProvider.getApplicationContext();
    if (deviceSupportsHdrEditing(VIDEO_H265, HDR10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device supports HDR10 editing.");
      return;
    }

    AtomicBoolean isFallbackListenerInvoked = new AtomicBoolean();
    AtomicBoolean isToneMappingFallbackApplied = new AtomicBoolean();
    Transformer transformer =
        new Transformer.Builder(context)
            .addListener(
                new Transformer.Listener() {
                  @Override
                  public void onFallbackApplied(
                      MediaItem inputMediaItem,
                      TransformationRequest originalTransformationRequest,
                      TransformationRequest fallbackTransformationRequest) {
                    isFallbackListenerInvoked.set(true);
                    assertThat(originalTransformationRequest.hdrMode)
                        .isEqualTo(TransformationRequest.HDR_MODE_KEEP_HDR);
                    isToneMappingFallbackApplied.set(
                        fallbackTransformationRequest.hdrMode
                            == TransformationRequest.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC);
                  }
                })
            .build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_720P_4_SECOND_HDR10));
    ImmutableList<Effect> videoEffects =
        ImmutableList.of(
            new ScaleAndRotateTransformation.Builder().setRotationDegrees(180).build());
    Effects effects = new Effects(/* audioProcessors= */ ImmutableList.of(), videoEffects);
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(effects).build();

    try {
      ExportTestResult exportTestResult =
          new TransformerAndroidTestRunner.Builder(context, transformer)
              .build()
              .run(testId, editedMediaItem);
      Log.i(TAG, "Tone mapped.");
      assertThat(isToneMappingFallbackApplied.get()).isTrue();
      maybeAssertFileHasColorTransfer(exportTestResult.filePath, C.COLOR_TRANSFER_SDR);
    } catch (ExportException exception) {
      Log.i(TAG, checkNotNull(exception.getCause()).toString());
      assertThat(exception).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
      assertThat(exception.errorCode)
          .isEqualTo(ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED);
      assertThat(isFallbackListenerInvoked.get()).isFalse();
    }
  }

  @Test
  public void exportAndTranscode_hlg10File_whenHdrEditingUnsupported_toneMapsOrThrows()
      throws Exception {
    String testId = "exportAndTranscode_hlg10File_whenHdrEditingUnsupported_toneMapsOrThrows";
    Context context = ApplicationProvider.getApplicationContext();
    if (deviceSupportsHdrEditing(VIDEO_H265, HLG10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device supports HLG10 editing.");
      return;
    }

    AtomicBoolean isFallbackListenerInvoked = new AtomicBoolean();
    AtomicBoolean isToneMappingFallbackApplied = new AtomicBoolean();
    Transformer transformer =
        new Transformer.Builder(context)
            .addListener(
                new Transformer.Listener() {
                  @Override
                  public void onFallbackApplied(
                      MediaItem inputMediaItem,
                      TransformationRequest originalTransformationRequest,
                      TransformationRequest fallbackTransformationRequest) {
                    isFallbackListenerInvoked.set(true);
                    assertThat(originalTransformationRequest.hdrMode)
                        .isEqualTo(TransformationRequest.HDR_MODE_KEEP_HDR);
                    isToneMappingFallbackApplied.set(
                        fallbackTransformationRequest.hdrMode
                            == TransformationRequest.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC);
                  }
                })
            .build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_5_SECOND_HLG10));
    ImmutableList<Effect> videoEffects =
        ImmutableList.of(
            new ScaleAndRotateTransformation.Builder().setRotationDegrees(180).build());
    Effects effects = new Effects(/* audioProcessors= */ ImmutableList.of(), videoEffects);
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(effects).build();

    try {
      ExportTestResult exportTestResult =
          new TransformerAndroidTestRunner.Builder(context, transformer)
              .build()
              .run(testId, editedMediaItem);
      Log.i(TAG, "Tone mapped.");
      assertThat(isToneMappingFallbackApplied.get()).isTrue();
      maybeAssertFileHasColorTransfer(exportTestResult.filePath, C.COLOR_TRANSFER_SDR);
    } catch (ExportException exception) {
      Log.i(TAG, checkNotNull(exception.getCause()).toString());
      assertThat(exception).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
      assertThat(exception.errorCode)
          .isEqualTo(ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED);
      assertThat(isFallbackListenerInvoked.get()).isFalse();
    }
  }

  @Test
  public void exportUnexpectedColorInfo() throws Exception {
    String testId = "exportUnexpectedColorInfo";
    Context context = ApplicationProvider.getApplicationContext();

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_1_SECOND_HDR10_VIDEO_SDR_CONTAINER));
    try {
      new TransformerAndroidTestRunner.Builder(context, transformer).build().run(testId, mediaItem);
    } catch (ExportException exception) {
      Log.i(TAG, checkNotNull(exception.getCause()).toString());
      assertThat(exception).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
      assertThat(exception.errorCode)
          .isEqualTo(ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED);
    }
  }

  private static boolean deviceSupportsHdrEditing(String mimeType, ColorInfo colorInfo) {
    return !EncoderUtil.getSupportedEncodersForHdrEditing(mimeType, colorInfo).isEmpty();
  }
}
