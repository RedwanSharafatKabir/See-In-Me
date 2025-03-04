/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;

/** Golden test for Image Classification Reference app. */
@RunWith(AndroidJUnit4.class)
public class ClassifierTest {

  @Rule
  public ActivityTestRule<ClassifierActivity> rule =
      new ActivityTestRule<>(ClassifierActivity.class);

  private static final String[] INPUTS = {"fox.jpg"};
  private static final String[] GOLDEN_OUTPUTS = {"fox-mobilenet_v1_1.0_224.txt"};

  @Test
  public void classificationResultsShouldNotChange() throws IOException {
    ClassifierActivity activity = rule.getActivity();
    Classifier classifier = Classifier.create(activity, Model.FLOAT, Device.CPU, 1);
    for (int i = 0; i < INPUTS.length; i++) {
      String imageFileName = INPUTS[i];
      String goldenOutputFileName = GOLDEN_OUTPUTS[i];
      Bitmap input = loadImage(imageFileName);
      List<Recognition> goldenOutput = loadRecognitions(goldenOutputFileName);

      List<Recognition> result = classifier.recognizeImage(input, 0);
      Iterator<Recognition> goldenOutputIterator = goldenOutput.iterator();

      for (Recognition actual : result) {
        Assert.assertTrue(goldenOutputIterator.hasNext());
        Recognition expected = goldenOutputIterator.next();
        assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
        assertThat(actual.getConfidence()).isWithin(0.01f).of(expected.getConfidence());
      }
    }
  }

  private static Bitmap loadImage(String fileName) {
    AssetManager assetManager =
        InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    InputStream inputStream = null;
    try {
      inputStream = assetManager.open(fileName);
    } catch (IOException e) {
      Log.e("Test", "Cannot load image from assets");
    }
    return BitmapFactory.decodeStream(inputStream);
  }

  private static List<Recognition> loadRecognitions(String fileName) {
    AssetManager assetManager =
        InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    InputStream inputStream = null;
    try {
      inputStream = assetManager.open(fileName);
    } catch (IOException e) {
      Log.e("Test", "Cannot load probability results from assets");
    }
    Scanner scanner = new Scanner(inputStream);
    List<Recognition> result = new ArrayList<>();
    while (scanner.hasNext()) {
      String category = scanner.next();
      category = category.replace('_', ' ');
      if (!scanner.hasNextFloat()) {
        break;
      }
      float probability = scanner.nextFloat();
      Recognition recognition = new Recognition(null, category, probability, null);
      result.add(recognition);
    }
    return result;
  }
}
