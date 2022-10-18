/*
 * Copyright (C) 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.util;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class AutoPatchUtilTest {

  private static final String PATCH_FILE_PREFIX = "This is a patch file generated by Copybara!\n";
  private static final String PATCH_FILE_NAME_SUFFIX = ".patch";
  private static final String SOME_DIR = "some/dir";
  private static final boolean VERBOSE = true;

  // Command requires the working dir as a File, and Jimfs does not support Path.toFile()
  @Rule public final TemporaryFolder tmpFolder = new TemporaryFolder();
  private Path left;
  private Path right;
  private Path out;

  @Before
  public void setUp() throws IOException {
    Path rootPath = tmpFolder.getRoot().toPath();
    left = createDir(rootPath, "left");
    right = createDir(rootPath, "right");
    out = createDir(rootPath, "out");
  }

  @Test
  public void patchFilesGeneratedAndWritten() throws Exception {
    writeFile(left, SOME_DIR.concat("/file1.txt"), "foo-left");
    writeFile(left, SOME_DIR.concat("/file2.txt"), "bar-left");
    writeFile(right, SOME_DIR.concat("/file1.txt"), "foo-right");
    writeFile(right, SOME_DIR.concat("/file2.txt"), "bar-right");

    AutoPatchUtil.generatePatchFiles(
        left,
        right,
        out,
        VERBOSE,
        System.getenv(),
        PATCH_FILE_PREFIX,
        PATCH_FILE_NAME_SUFFIX,
        Path.of(SOME_DIR),
        true);

    assertThat(Files.readString(out.resolve("file1.txt".concat(PATCH_FILE_NAME_SUFFIX))))
        .isEqualTo(
            PATCH_FILE_PREFIX.concat(
                "@@\n"
                    + "-foo-left\n"
                    + "\\ No newline at end of file\n"
                    + "+foo-right\n"
                    + "\\ No newline at end of file\n"));
    assertThat(Files.readString(out.resolve("file2.txt".concat(PATCH_FILE_NAME_SUFFIX))))
        .isEqualTo(
            PATCH_FILE_PREFIX.concat(
                "@@\n"
                    + "-bar-left\n"
                    + "\\ No newline at end of file\n"
                    + "+bar-right\n"
                    + "\\ No newline at end of file\n"));
  }

  @Test
  public void emptyDiffGeneratesNoPatchFiles() throws Exception {
    writeFile(left, SOME_DIR.concat("/file1.txt"), "foo");
    writeFile(left, SOME_DIR.concat("/b/file2.txt"), "bar");
    writeFile(right, SOME_DIR.concat("/file1.txt"), "foo");
    writeFile(right, SOME_DIR.concat("/b/file2.txt"), "bar");

    AutoPatchUtil.generatePatchFiles(
        left,
        right,
        out,
        VERBOSE,
        System.getenv(),
        PATCH_FILE_PREFIX,
        PATCH_FILE_NAME_SUFFIX,
        Path.of(SOME_DIR),
        true);

    assertThat(Files.exists(out.resolve("/file1.txt".concat(PATCH_FILE_NAME_SUFFIX)))).isFalse();
    assertThat(Files.exists(out.resolve("/b/file2.txt".concat(PATCH_FILE_NAME_SUFFIX)))).isFalse();
  }

  private Path createDir(Path parent, String name) throws IOException {
    Path path = parent.resolve(name);
    Files.createDirectories(path);
    return path;
  }

  private void writeFile(Path parent, String fileName, String fileContents) throws IOException {
    Path filePath = parent.resolve(fileName);
    Files.createDirectories(filePath.getParent());
    Files.writeString(parent.resolve(filePath), fileContents);
  }
}
