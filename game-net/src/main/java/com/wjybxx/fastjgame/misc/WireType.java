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

package com.wjybxx.fastjgame.misc;


import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.enummapper.NumberEnum;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 序列化的数据类型。
 * 为何不支持数组类型[] ????，支持起来太费劲了，难受，请使用list代替。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
public class WireType {

	/**
	 * varInt, sInt
	 */
	public static final int BYTE = 1;
	/**
	 * uInt
	 */
	public static final int CHAR = 2;
	/**
	 * varInt, sInt
	 */
	public static final int SHORT = 3;
	/**
	 * varInt, sInt
	 */
	public static final int INT = 4;
	/**
	 * varInt64, sInt64
	 */
	public static final int LONG = 5;
	/**
	 * fixed32
	 */
	public static final int FLOAT = 6;
	/**
	 * fixed64
	 */
	public static final int DOUBLE = 7;
	/**
	 * uInt
	 */
	public static final int BOOLEAN = 8;

	/**
	 * 字符串 LENGTH_DELIMITED
	 */
	public static final int STRING = 9;
	/**
	 * protobuf LENGTH_DELIMITED
	 */
	public static final int MESSAGE = 10;
	/**
	 * protoBuf的枚举
	 */
	public static final int PROTO_ENUM = 11;
	/**
	 * 枚举支持，自定义枚举必须实现{@link com.wjybxx.fastjgame.enummapper.NumberEnum}接口，
	 * 且必须定义 forNumber(int) 获取枚举值的静态方法，以使得和protoBuf一样解析。
	 * 拆分为两个枚举是为了避免编码时的反射调用。
	 */
	public static final int NUMBER_ENUM = 12;

	// -- 基本集合
	/**
	 * List，解析时使用{@link java.util.ArrayList}，Repeatable
	 */
	public static final int LIST = 13;

	/**
	 * Set，解析时使用{@link java.util.LinkedHashSet}，保持顺序
	 */
	public static final int SET = 14;

	/**
	 * Map，解析时使用{@link java.util.LinkedHashMap}，保持顺序
	 */
	public static final int MAP = 15;
	/**
	 * NULL
	 */
	public static final int NULL = 16;
	/**
	 * 可序列化的普通对象，最好是简单的Bean -- POJO，必须带有{@link SerializableClass}注解。
	 * 必须有无参构造方法，可以是private。
	 */
	public static final int REFERENCE = 17;
	/**
	 * 它不代表内容，仅仅表示一个分隔符
	 */
	public static final int REFERENCE_END = 18;

	/**
	 * 查找一个class对应的wireType
	 * @param type class
	 * @return wireType
	 */
	public static int findType(@Nonnull final Class<?> type) {
		// --- 基本类型
		if (type == byte.class || type == Byte.class) {
			return WireType.BYTE;
		}

		if (type == char.class || type == Character.class) {
			return WireType.CHAR;
		}

		if (type == short.class || type == Short.class) {
			return WireType.SHORT;
		}

		if (type == int.class || type == Integer.class) {
			return WireType.INT;
		}

		if (type == long.class || type == Long.class) {
			return WireType.LONG;
		}

		if (type == float.class || type == Float.class) {
			return WireType.FLOAT;
		}

		if (type == double.class || type == Double.class) {
			return WireType.DOUBLE;
		}

		if (type == boolean.class || type == Boolean.class) {
			return WireType.BOOLEAN;
		}
		// 字符串
		if (type == String.class) {
			return WireType.STRING;
		}
		// protoBuf
		if (AbstractMessage.class.isAssignableFrom(type)) {
			return WireType.MESSAGE;
		}
		// NumberEnum枚举 -- 不一定真的是枚举
		if (NumberEnum.class.isAssignableFrom(type)) {
			return WireType.NUMBER_ENUM;
		}
		// protoBuf的枚举
		if (ProtocolMessageEnum.class.isAssignableFrom(type)) {
			return WireType.PROTO_ENUM;
		}

		// 常用集合支持
		if (List.class.isAssignableFrom(type)) {
			return WireType.LIST;
		}
		// Set
		if (Set.class.isAssignableFrom(type)) {
			return WireType.SET;
		}
		// Map
		if (Map.class.isAssignableFrom(type)) {
			return WireType.MAP;
		}
		// 自定义类型
		if (type.isAnnotationPresent(SerializableClass.class)) {
			return WireType.REFERENCE;
		}
		throw new IllegalArgumentException("unsupported type " + type.getName());
	}
}
