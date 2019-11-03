/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.text.ssa;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.testutil.TestUtil;
import com.google.android.exoplayer2.text.Subtitle;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit test for {@link SsaDecoder}. */
@RunWith(AndroidJUnit4.class)
public final class SsaDecoderTest {

  private static final String EMPTY = "ssa/empty";
  private static final String TYPICAL = "ssa/typical";
  private static final String OVERLAP = "ssa/overlap";
  private static final String TYPICAL_HEADER_ONLY = "ssa/typical_header";
  private static final String TYPICAL_DIALOGUE_ONLY = "ssa/typical_dialogue";
  private static final String TYPICAL_FORMAT_ONLY = "ssa/typical_format";
  private static final String INVALID_TIMECODES = "ssa/invalid_timecodes";
  private static final String NO_END_TIMECODES = "ssa/no_end_timecodes";

  @Test
  public void testDecodeEmpty() throws IOException {
    SsaDecoder decoder = new SsaDecoder();
    byte[] bytes = TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), EMPTY);
    Subtitle subtitle = decoder.decode(bytes, bytes.length, false);

    assertThat(subtitle.getEventTimeCount()).isEqualTo(0);
    assertThat(subtitle.getCues(0).isEmpty()).isTrue();
  }

  @Test
  public void testDecodeTypical() throws IOException {
    SsaDecoder decoder = new SsaDecoder();
    byte[] bytes = TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), TYPICAL);
    Subtitle subtitle = decoder.decode(bytes, bytes.length, false);

    assertThat(subtitle.getEventTimeCount()).isEqualTo(6);
    assertTypicalCue1(subtitle, 0);
    assertTypicalCue2(subtitle, 2);
    assertTypicalCue3(subtitle, 4);
  }

  @Test
  public void testDecodeOverlap() throws IOException {
    SsaDecoder decoder = new SsaDecoder();
    byte[] bytes = TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), OVERLAP);
    Subtitle subtitle = decoder.decode(bytes, bytes.length, false);

    assertThat(subtitle.getEventTime(0)).isEqualTo(1000000);
    assertThat(subtitle.getEventTime(1)).isEqualTo(2000000);
    assertThat(subtitle.getEventTime(2)).isEqualTo(4230000);
    assertThat(subtitle.getEventTime(3)).isEqualTo(5230000);
    assertThat(subtitle.getEventTime(4)).isEqualTo(6000000);
    assertThat(subtitle.getEventTime(5)).isEqualTo(8440000);
    assertThat(subtitle.getEventTime(6)).isEqualTo(9440000);
    assertThat(subtitle.getEventTime(7)).isEqualTo(10720000);
    assertThat(subtitle.getEventTime(8)).isEqualTo(13220000);
    assertThat(subtitle.getEventTime(9)).isEqualTo(14220000);
    assertThat(subtitle.getEventTime(10)).isEqualTo(15650000);

    assertThat(subtitle.getCues(1000010).size()).isEqualTo(1);
    assertThat(subtitle.getCues(2000010).size()).isEqualTo(2);
    assertThat(subtitle.getCues(4230010).size()).isEqualTo(1);
    assertThat(subtitle.getCues(5230010).size()).isEqualTo(0);
    assertThat(subtitle.getCues(6000010).size()).isEqualTo(1);
    assertThat(subtitle.getCues(8440010).size()).isEqualTo(2);
    assertThat(subtitle.getCues(9440010).size()).isEqualTo(0);
    assertThat(subtitle.getCues(10720010).size()).isEqualTo(1);
    assertThat(subtitle.getCues(13220010).size()).isEqualTo(2);
    assertThat(subtitle.getCues(14220010).size()).isEqualTo(1);
    assertThat(subtitle.getCues(15650010).size()).isEqualTo(0);
  }

  @Test
  public void testDecodeTypicalWithInitializationData() throws IOException {
    byte[] headerBytes =
        TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), TYPICAL_HEADER_ONLY);
    byte[] formatBytes =
        TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), TYPICAL_FORMAT_ONLY);
    ArrayList<byte[]> initializationData = new ArrayList<>();
    initializationData.add(formatBytes);
    initializationData.add(headerBytes);
    SsaDecoder decoder = new SsaDecoder(initializationData);
    byte[] bytes =
        TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), TYPICAL_DIALOGUE_ONLY);
    Subtitle subtitle = decoder.decode(bytes, bytes.length, false);

    assertThat(subtitle.getEventTimeCount()).isEqualTo(6);
    assertTypicalCue1(subtitle, 0);
    assertTypicalCue2(subtitle, 2);
    assertTypicalCue3(subtitle, 4);
  }

  @Test
  public void testDecodeInvalidTimecodes() throws IOException {
    // Parsing should succeed, parsing the third cue only.
    SsaDecoder decoder = new SsaDecoder();
    byte[] bytes =
        TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), INVALID_TIMECODES);
    Subtitle subtitle = decoder.decode(bytes, bytes.length, false);

    assertThat(subtitle.getEventTimeCount()).isEqualTo(2);
    assertTypicalCue3(subtitle, 0);
  }

  @Test
  public void testDecodeNoEndTimecodes() throws IOException {
    SsaDecoder decoder = new SsaDecoder();
    byte[] bytes =
        TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), NO_END_TIMECODES);
    Subtitle subtitle = decoder.decode(bytes, bytes.length, false);

    assertThat(subtitle.getEventTimeCount()).isEqualTo(3);

    assertThat(subtitle.getEventTime(0)).isEqualTo(0);
    assertThat(subtitle.getCues(subtitle.getEventTime(0)).get(0).text.toString())
        .isEqualTo("This is the first subtitle.");

    assertThat(subtitle.getEventTime(1)).isEqualTo(2340000);
    assertThat(subtitle.getCues(subtitle.getEventTime(1)).get(0).text.toString())
        .isEqualTo("This is the first subtitle.");
    assertThat(subtitle.getCues(subtitle.getEventTime(1)).get(1).text.toString())
        .isEqualTo("This is the second subtitle \nwith a newline \nand another.");

    assertThat(subtitle.getEventTime(2)).isEqualTo(4560000);
    assertThat(subtitle.getCues(subtitle.getEventTime(1)).get(0).text.toString())
        .isEqualTo("This is the first subtitle.");
    assertThat(subtitle.getCues(subtitle.getEventTime(1)).get(1).text.toString())
        .isEqualTo("This is the second subtitle \nwith a newline \nand another.");
    assertThat(subtitle.getCues(subtitle.getEventTime(2)).get(2).text.toString())
        .isEqualTo("This is the third subtitle, with a comma.");
  }

  private static void assertTypicalCue1(Subtitle subtitle, int eventIndex) {
    assertThat(subtitle.getEventTime(eventIndex)).isEqualTo(0);
    assertThat(subtitle.getCues(subtitle.getEventTime(eventIndex)).get(0).text.toString())
        .isEqualTo("This is the first subtitle.");
    assertThat(subtitle.getEventTime(eventIndex + 1)).isEqualTo(1230000);
  }

  private static void assertTypicalCue2(Subtitle subtitle, int eventIndex) {
    assertThat(subtitle.getEventTime(eventIndex)).isEqualTo(2340000);
    assertThat(subtitle.getCues(subtitle.getEventTime(eventIndex)).get(0).text.toString())
        .isEqualTo("This is the second subtitle \nwith a newline \nand another.");
    assertThat(subtitle.getEventTime(eventIndex + 1)).isEqualTo(3450000);
  }

  private static void assertTypicalCue3(Subtitle subtitle, int eventIndex) {
    assertThat(subtitle.getEventTime(eventIndex)).isEqualTo(4560000);
    assertThat(subtitle.getCues(subtitle.getEventTime(eventIndex)).get(0).text.toString())
        .isEqualTo("This is the third subtitle, with a comma.");
    assertThat(subtitle.getEventTime(eventIndex + 1)).isEqualTo(8900000);
  }
}
