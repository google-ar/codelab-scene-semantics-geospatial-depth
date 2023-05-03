#version 310 es
/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

precision mediump float;
uniform sampler2D uConfidenceImage;

in vec2 vTextureCoord;
out vec4 oFragColor;

void main() {
  // Get pixel value from confidence image.
  float pixel_value = texture(uConfidenceImage, vTextureCoord.xy).r;

  vec3 color = vec3(pixel_value);

  // Set alpha value to allow some transparency.
  vec4 confidence_color = vec4(color, 0.8);

  oFragColor = confidence_color;
}