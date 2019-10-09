/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotation.SerializableField;
import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 示例消息类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public final class ExampleMessages {

    @SerializableClass
    public static class Hello {
        /**
         * 消息id
         */
        @SerializableField(number = 1)
        private long id;
        /**
         * 消息内容
         */
        @SerializableField(number = 2)
        private String message;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
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

        @SerializableField(number = 0)
        private Object any;

        @SerializableField(number = 1)
        private byte aByte;

        @SerializableField(number = 2)
        private char aChar;

        @SerializableField(number = 3)
        private short aShort;

        @SerializableField(number = 4)
        private int anInt;

        @SerializableField(number = 5)
        private long aLong;

        @SerializableField(number = 6)
        private float aFloat;

        @SerializableField(number = 7)
        private double aDouble;

        @SerializableField(number = 8)
        private boolean aBoolean;

        @SerializableField(number = 9)
        private String name;

        @SerializableField(number = 10)
        private Profession profession;

        @SerializableField(number = 11)
        private List<String> stringList;

        @SerializableField(number = 12)
        private Set<String> stringSet;

        @SerializableField(number = 13)
        private Map<String, String> stringStringMap;

        @SerializableField(number = 14)
        private Hello hello;

        @SerializableField(number = 15)
        private String aNull;

        @SerializableField(number = 16)
        private byte[] aByteArray;

        @SerializableField(number = 17)
        private short[] aShortArray;

        @SerializableField(number = 18)
        private int[] aIntArray;

        @SerializableField(number = 19)
        private long[] aLongArrray;

        @SerializableField(number = 20)
        private float[] aFloatArray;

        @SerializableField(number = 21)
        private double[] aDoubleArray;

        @SerializableField(number = 22)
        private char[] aCharArray;

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
    public enum Profession implements NumberEnum {
        CODER(1),
        TEACHER(2),
        ;

        private int number;

        Profession(int number) {
            this.number = number;
        }

        private static NumberEnumMapper<Profession> mapper = EnumUtils.indexNumberEnum(values());

        @Override
        public int getNumber() {
            return number;
        }

        public static Profession forNumber(int number) {
            return mapper.forNumber(number);
        }
    }

}
