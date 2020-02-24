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

import com.wjybxx.fastjgame.db.annotation.DBEntity;
import com.wjybxx.fastjgame.db.annotation.DBField;
import com.wjybxx.fastjgame.db.annotation.Impl;
import com.wjybxx.fastjgame.net.annotation.SerializableClass;
import com.wjybxx.fastjgame.net.annotation.SerializableField;
import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.entity.IndexableEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntity;
import com.wjybxx.fastjgame.utils.entity.NumericalEntityMapper;
import com.wjybxx.fastjgame.utils.misc.IntPair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
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

    /**
     * 测试抽象类的生成代码
     */
    @SerializableClass
    public static abstract class AbstractMsg {

        @SerializableField
        private int type;

        @SerializableField
        private int id;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    @SerializableClass
    public static class SceneConfig implements IndexableEntity<IntPair> {

        private final int type;
        private final int id;

        private SceneConfig() {
            type = 0;
            id = 0;
        }

        public SceneConfig(int type, int id) {
            this.type = type;
            this.id = id;
        }

        @Nonnull
        @Override
        public IntPair getIndex() {
            return new IntPair(type, id);
        }

        static SceneConfig forIndex(IntPair key) {
            // 当做查询就好
            return new SceneConfig(key.getFirst(), key.getSecond());
        }
    }


    @DBEntity(name = "db_bean")
    public static class DBBean {

        @DBField(name = "guid")
        private long guid;

        @DBField(name = "name")
        private String name;

        @SerializableField
        private String sex;

        @SerializableField
        private boolean success;

        @SerializableField
        private Boolean ok;

        public long getGuid() {
            return guid;
        }

        public void setGuid(long guid) {
            this.guid = guid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Boolean getOk() {
            return ok;
        }

        public void setOk(Boolean ok) {
            this.ok = ok;
        }
    }


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
    public static class FullMessage extends Hello {

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
        private String aString;

        @SerializableField
        private Profession profession;

        @Impl(ArrayList.class)
        @SerializableField
        private List<String> stringList;

        @Impl(HashSet.class)
        @SerializableField
        private Set<String> stringSet;

        @Impl(LinkedHashMap.class)
        @SerializableField
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

        @SerializableField
        private String[] aStringArray;

        @SerializableField
        private Class[] aClassArray;

        @SerializableField
        private String[][] twoDimensionsStringArray;

        @Impl(Int2ObjectOpenHashMap.class)
        @SerializableField
        private Int2ObjectMap<String> int2ObjectMap;

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

        public String getaString() {
            return aString;
        }

        public void setaString(String aString) {
            this.aString = aString;
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

        public Int2ObjectMap<String> getInt2ObjectMap() {
            return int2ObjectMap;
        }

        public void setInt2ObjectMap(Int2ObjectMap<String> int2ObjectMap) {
            this.int2ObjectMap = int2ObjectMap;
        }

        public String[] getaStringArray() {
            return aStringArray;
        }

        public void setaStringArray(String[] aStringArray) {
            this.aStringArray = aStringArray;
        }

        public Class[] getaClassArray() {
            return aClassArray;
        }

        public void setaClassArray(Class[] aClassArray) {
            this.aClassArray = aClassArray;
        }

        public String[][] getTwoDimensionsStringArray() {
            return twoDimensionsStringArray;
        }

        public void setTwoDimensionsStringArray(String[][] twoDimensionsStringArray) {
            this.twoDimensionsStringArray = twoDimensionsStringArray;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("any", any)
                    .append("aByte", aByte)
                    .append("aChar", aChar)
                    .append("aShort", aShort)
                    .append("anInt", anInt)
                    .append("aLong", aLong)
                    .append("aFloat", aFloat)
                    .append("aDouble", aDouble)
                    .append("aBoolean", aBoolean)
                    .append("aString", aString)
                    .append("profession", profession)
                    .append("stringList", stringList)
                    .append("stringSet", stringSet)
                    .append("stringStringMap", stringStringMap)
                    .append("hello", hello)
                    .append("aNull", aNull)
                    .append("aByteArray", aByteArray)
                    .append("aShortArray", aShortArray)
                    .append("aIntArray", aIntArray)
                    .append("aLongArrray", aLongArrray)
                    .append("aFloatArray", aFloatArray)
                    .append("aDoubleArray", aDoubleArray)
                    .append("aCharArray", aCharArray)
                    .append("aStringArray", aStringArray)
                    .append("aClassArray", aClassArray)
                    .append("twoDimensionsStringArray", twoDimensionsStringArray)
                    .append("int2ObjectMap", int2ObjectMap)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            FullMessage that = (FullMessage) o;

            return new EqualsBuilder()
                    .appendSuper(super.equals(o))
                    .append(aByte, that.aByte)
                    .append(aChar, that.aChar)
                    .append(aShort, that.aShort)
                    .append(anInt, that.anInt)
                    .append(aLong, that.aLong)
                    .append(aFloat, that.aFloat)
                    .append(aDouble, that.aDouble)
                    .append(aBoolean, that.aBoolean)
                    .append(any, that.any)
                    .append(aString, that.aString)
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
                    .append(aStringArray, that.aStringArray)
                    .append(aClassArray, that.aClassArray)
                    .append(twoDimensionsStringArray, that.twoDimensionsStringArray)
                    .append(int2ObjectMap, that.int2ObjectMap)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .appendSuper(super.hashCode())
                    .append(any)
                    .append(aByte)
                    .append(aChar)
                    .append(aShort)
                    .append(anInt)
                    .append(aLong)
                    .append(aFloat)
                    .append(aDouble)
                    .append(aBoolean)
                    .append(aString)
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
                    .append(aStringArray)
                    .append(aClassArray)
                    .append(twoDimensionsStringArray)
                    .append(int2ObjectMap)
                    .toHashCode();
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
