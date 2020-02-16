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
package com.wjybxx.fastjgame.net.example;

import com.wjybxx.fastjgame.net.annotation.SerializableClass;
import com.wjybxx.fastjgame.net.annotation.SerializableField;
import com.wjybxx.fastjgame.net.misc.*;
import com.wjybxx.fastjgame.net.serializer.BeanInputStream;
import com.wjybxx.fastjgame.net.serializer.BeanOutputStream;
import com.wjybxx.fastjgame.net.serializer.BeanSerializer;
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.util.*;

/**
 * 示例消息类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public final class ExampleMessages {

    // 它会被扫描到，并负责hello类的解析
    public static class HelloSerializer implements BeanSerializer<Hello> {

        public HelloSerializer() {
            System.out.println("HelloSerializer constructor");
        }

        @Override
        public void writeFields(Hello instance, BeanOutputStream outputStream) throws IOException {
            outputStream.writeField(WireType.LONG, instance.id);
            outputStream.writeField(WireType.STRING, instance.message);
        }

        @Override
        public Hello read(BeanInputStream inputStream) throws IOException {
            final Long id = inputStream.readField(WireType.LONG);
            final String message = inputStream.readField(WireType.STRING);
            return new Hello(id, message);
        }

        @Override
        public Hello clone(Hello instance, BeanCloneUtil util) throws IOException {
            return new Hello(util.cloneField(WireType.LONG, instance.id), util.cloneField(WireType.STRING, instance.message));
        }
    }

    /**
     * 它不是一个标准的javabean，因此不能自动生成对应的编解码类。
     * 我们可以手动写一个代替反射
     */
    @SerializableClass
    public static class Hello {
        /**
         * 消息id
         */
        @SerializableField
        private final long id;
        /**
         * 消息内容
         */
        @SerializableField
        private final String message;

        private Hello() {
            id = 0;
            message = null;
        }

        public Hello(long id, String message) {
            this.id = id;
            this.message = message;
        }

        public long getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;

            if (object == null || getClass() != object.getClass()) return false;

            Hello hello = (Hello) object;

            return new EqualsBuilder()
                    .append(id, hello.id)
                    .append(message, hello.message)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(id)
                    .append(message)
                    .toHashCode();
        }
    }

    @SerializableClass
    public static class FullMessage {

        @SerializableField
        private Object any;

        @SerializableField
        private byte aByte;

        @SerializableField
        private char aChar;

        @SerializableField
        private short aShort;

        @SerializableField
        private int anInt;

        @SerializableField
        private long aLong;

        @SerializableField
        private float aFloat;

        @SerializableField
        private double aDouble;

        @SerializableField
        private boolean aBoolean;

        @SerializableField
        private String name;

        @SerializableField
        private Profession profession;

        @SerializableField(impl = ArrayList.class)
        private List<String> stringList;

        @SerializableField(impl = HashSet.class)
        private Set<String> stringSet;

        @SerializableField(impl = LinkedHashMap.class)
        private Map<String, String> stringStringMap;

        @SerializableField
        private Hello hello;

        @SerializableField
        private String aNull;

        @SerializableField
        private byte[] aByteArray;

        @SerializableField
        private short[] aShortArray;

        @SerializableField
        private int[] aIntArray;

        @SerializableField
        private long[] aLongArrray;

        @SerializableField
        private float[] aFloatArray;

        @SerializableField
        private double[] aDoubleArray;

        @SerializableField
        private char[] aCharArray;

        public FullMessage() {
        }

        public Object getAny() {
            return any;
        }

        public void setAny(Object any) {
            this.any = any;
        }

        public byte getaByte() {
            return aByte;
        }

        public void setaByte(byte aByte) {
            this.aByte = aByte;
        }

        public char getaChar() {
            return aChar;
        }

        public void setaChar(char aChar) {
            this.aChar = aChar;
        }

        public short getaShort() {
            return aShort;
        }

        public void setaShort(short aShort) {
            this.aShort = aShort;
        }

        public int getAnInt() {
            return anInt;
        }

        public void setAnInt(int anInt) {
            this.anInt = anInt;
        }

        public long getaLong() {
            return aLong;
        }

        public void setaLong(long aLong) {
            this.aLong = aLong;
        }

        public float getaFloat() {
            return aFloat;
        }

        public void setaFloat(float aFloat) {
            this.aFloat = aFloat;
        }

        public double getaDouble() {
            return aDouble;
        }

        public void setaDouble(double aDouble) {
            this.aDouble = aDouble;
        }

        public boolean isaBoolean() {
            return aBoolean;
        }

        public void setaBoolean(boolean aBoolean) {
            this.aBoolean = aBoolean;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Profession getProfession() {
            return profession;
        }

        public void setProfession(Profession profession) {
            this.profession = profession;
        }

        public List<String> getStringList() {
            return stringList;
        }

        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

        public Set<String> getStringSet() {
            return stringSet;
        }

        public void setStringSet(Set<String> stringSet) {
            this.stringSet = stringSet;
        }

        public Map<String, String> getStringStringMap() {
            return stringStringMap;
        }

        public void setStringStringMap(Map<String, String> stringStringMap) {
            this.stringStringMap = stringStringMap;
        }

        public Hello getHello() {
            return hello;
        }

        public void setHello(Hello hello) {
            this.hello = hello;
        }

        public String getaNull() {
            return aNull;
        }

        public void setaNull(String aNull) {
            this.aNull = aNull;
        }

        public byte[] getaByteArray() {
            return aByteArray;
        }

        public void setaByteArray(byte[] aByteArray) {
            this.aByteArray = aByteArray;
        }


        public short[] getaShortArray() {
            return aShortArray;
        }

        public void setaShortArray(short[] aShortArray) {
            this.aShortArray = aShortArray;
        }

        public int[] getaIntArray() {
            return aIntArray;
        }

        public void setaIntArray(int[] aIntArray) {
            this.aIntArray = aIntArray;
        }

        public long[] getaLongArrray() {
            return aLongArrray;
        }

        public void setaLongArrray(long[] aLongArrray) {
            this.aLongArrray = aLongArrray;
        }

        public float[] getaFloatArray() {
            return aFloatArray;
        }

        public void setaFloatArray(float[] aFloatArray) {
            this.aFloatArray = aFloatArray;
        }

        public double[] getaDoubleArray() {
            return aDoubleArray;
        }

        public void setaDoubleArray(double[] aDoubleArray) {
            this.aDoubleArray = aDoubleArray;
        }

        public char[] getaCharArray() {
            return aCharArray;
        }

        public void setaCharArray(char[] aCharArray) {
            this.aCharArray = aCharArray;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            FullMessage that = (FullMessage) o;

            return new EqualsBuilder()
                    .append(aByte, that.aByte)
                    .append(aChar, that.aChar)
                    .append(aShort, that.aShort)
                    .append(anInt, that.anInt)
                    .append(aLong, that.aLong)
                    .append(aFloat, that.aFloat)
                    .append(aDouble, that.aDouble)
                    .append(aBoolean, that.aBoolean)
                    .append(any, that.any)
                    .append(name, that.name)
                    .append(profession, that.profession)
                    .append(stringList, that.stringList)
                    .append(stringSet, that.stringSet)
                    .append(stringStringMap, that.stringStringMap)
                    .append(hello, that.hello)
                    .append(aNull, that.aNull)
                    .append(aByteArray, that.aByteArray)
                    .append(aShortArray, that.aShortArray)
                    .append(aIntArray, that.aIntArray)
                    .append(aLongArrray, that.aLongArrray)
                    .append(aFloatArray, that.aFloatArray)
                    .append(aDoubleArray, that.aDoubleArray)
                    .append(aCharArray, that.aCharArray)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(any)
                    .append(aByte)
                    .append(aChar)
                    .append(aShort)
                    .append(anInt)
                    .append(aLong)
                    .append(aFloat)
                    .append(aDouble)
                    .append(aBoolean)
                    .append(name)
                    .append(profession)
                    .append(stringList)
                    .append(stringSet)
                    .append(stringStringMap)
                    .append(hello)
                    .append(aNull)
                    .append(aByteArray)
                    .append(aShortArray)
                    .append(aIntArray)
                    .append(aLongArrray)
                    .append(aFloatArray)
                    .append(aDoubleArray)
                    .append(aCharArray)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return "FullMessage{" +
                    "any=" + any +
                    ", aByte=" + aByte +
                    ", aChar=" + aChar +
                    ", aShort=" + aShort +
                    ", anInt=" + anInt +
                    ", aLong=" + aLong +
                    ", aFloat=" + aFloat +
                    ", aDouble=" + aDouble +
                    ", aBoolean=" + aBoolean +
                    ", name='" + name + '\'' +
                    ", profession=" + profession +
                    ", stringList=" + stringList +
                    ", stringSet=" + stringSet +
                    ", stringStringMap=" + stringStringMap +
                    ", hello=" + hello +
                    ", aNull='" + aNull + '\'' +
                    ", aByteArray=" + Arrays.toString(aByteArray) +
                    ", aShortArray=" + Arrays.toString(aShortArray) +
                    ", aIntArray=" + Arrays.toString(aIntArray) +
                    ", aLongArrray=" + Arrays.toString(aLongArrray) +
                    ", aFloatArray=" + Arrays.toString(aFloatArray) +
                    ", aDoubleArray=" + Arrays.toString(aDoubleArray) +
                    ", aCharArray=" + Arrays.toString(aCharArray) +
                    '}';
        }
    }

    @SerializableClass
    public enum Profession implements NumericalEntity {
        CODER(1),
        TEACHER(2),
        ;

        private int number;

        Profession(int number) {
            this.number = number;
        }

        private static NumericalEntityMapper<Profession> mapper = EnumUtils.mapping(values());

        @Override
        public int getNumber() {
            return number;
        }

        public static Profession forNumber(int number) {
            return mapper.forNumber(number);
        }
    }

}
