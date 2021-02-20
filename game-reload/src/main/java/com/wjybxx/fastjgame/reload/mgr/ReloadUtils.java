/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.reload.mgr;


import com.wjybxx.fastjgame.util.CaseMode;
import com.wjybxx.fastjgame.util.CodecUtils;
import com.wjybxx.fastjgame.util.constant.BadImplementationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author wjybxx
 * date - 2020/11/17
 * github - https://github.com/hl845740757
 */
class ReloadUtils {

    static <T> void ensureSameType(T protoType, T cloned) {
        if (protoType.getClass() != cloned.getClass()) {
            final String msg = String.format("protoType: %s, cloned: %s", protoType.getClass().getName(), cloned.getClass().getName());
            throw new BadImplementationException(msg);
        }
    }

    static byte[] readBytes(File file) throws IOException {
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            return inputStream.readAllBytes();
        }
    }

    /**
     * @param file        要测试的文件
     * @param oldFileStat 文件的当前状态，如果为null，一定返回新的stat
     * @return 如果文件的状态没有发生变化，则返回当前的stat，否则返回新的stat
     */
    static FileStat statOfFile(File file, @Nullable FileStat oldFileStat) throws IOException {
        if (null == oldFileStat) {
            return new FileStat(file.length(), CodecUtils.md5Hex(file, CaseMode.UPPER_CASE));
        }

        final long newLength = file.length();
        if (newLength != oldFileStat.length) {
            return new FileStat(newLength, CodecUtils.md5Hex(file, CaseMode.UPPER_CASE));
        }

        final String newMd5 = CodecUtils.md5Hex(file, CaseMode.UPPER_CASE);
        if (!newMd5.equals(oldFileStat.md5)) {
            return new FileStat(newLength, newMd5);
        }

        return oldFileStat;
    }

    /**
     * @param bytesOfFile 要测试的文件内容
     * @param oldFileStat 文件的当前状态，如果为null，一定返回新的stat
     * @return 如果文件的状态没有发生变化，则返回当前的stat，否则返回新的stat
     */
    static FileStat stateOfFileBytes(byte[] bytesOfFile, @Nullable FileStat oldFileStat) {
        if (null == oldFileStat) {
            return new FileStat(bytesOfFile.length, CodecUtils.md5Hex(bytesOfFile, CaseMode.UPPER_CASE));
        }

        final int newLength = bytesOfFile.length;
        if (newLength != oldFileStat.length) {
            return new FileStat(newLength, CodecUtils.md5Hex(bytesOfFile, CaseMode.UPPER_CASE));
        }

        final String newMd5 = CodecUtils.md5Hex(bytesOfFile, CaseMode.UPPER_CASE);
        if (!newMd5.equals(oldFileStat.md5)) {
            return new FileStat(newLength, newMd5);
        }

        return oldFileStat;
    }

    public static final class FileStat {

        private final long length;
        private final String md5;

        FileStat(long length, String md5) {
            this.length = length;
            this.md5 = md5;
        }

        // 必须实现equals，避免某些地方使用了equals进行比较
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final FileStat fileStat = (FileStat) o;
            return length == fileStat.length &&
                    md5.equals(fileStat.md5);
        }

        @Override
        public int hashCode() {
            return 31 * Long.hashCode(length) + md5.hashCode();
        }

    }

    /**
     * 递归搜索目录下的所有文件
     *
     * @param dir    文件夹
     * @param out    接收结果的列表，用于减少不必要的列表创建
     * @param filter 文件过滤器，可以为null
     */
    static void recurseDir(@Nonnull File dir, @Nonnull List<File> out, @Nullable Predicate<File> filter) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        final File[] children = dir.listFiles(file -> {
            final String fileName = file.getName();
            if (fileName.startsWith(".") || fileName.startsWith("~$")) {
                // 隐藏文件和临时文件
                return false;
            } else {
                return file.isDirectory() || null == filter || filter.test(file);
            }
        });

        if (null == children) {
            return;
        }

        for (File file : children) {
            if (file.isDirectory()) {
                recurseDir(file, out, filter);
            } else {
                out.add(file);
            }
        }
    }
}
