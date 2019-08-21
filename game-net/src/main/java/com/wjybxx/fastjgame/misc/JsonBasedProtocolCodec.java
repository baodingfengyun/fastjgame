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

import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.utils.JsonUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.buffer.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 基于Json的编解码器，必须使用简单对象来封装参数。-- POJO
 * 编码后的数据量较多，编解码效率也很低，建议只在测试期间使用。-- 因为json可读性很好，正式编解码时建议使用{@link ReflectBasedProtocolCodec}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
public class JsonBasedProtocolCodec implements ProtocolCodec {

	private final MessageMapper messageMapper;

	public JsonBasedProtocolCodec(MessageMapper messageMapper) {
		this.messageMapper = messageMapper;
	}

	@Override
	public ByteBuf encodeRpcRequest(ByteBufAllocator bufAllocator, Object request) throws IOException {
		return encode(bufAllocator, request);
	}

	@Override
	public Object decodeRpcRequest(ByteBuf data) throws IOException {
		return decode(data);
	}

	@Override
	public ByteBuf encodeRpcResponse(ByteBufAllocator bufAllocator, Object body) throws IOException {
		return encode(bufAllocator, body);
	}

	@Override
	public Object decodeRpcResponse(ByteBuf data) throws IOException {
		return decode(data);
	}

	@Override
	public ByteBuf encodeMessage(ByteBufAllocator bufAllocator, Object message) throws IOException {
		return encode(bufAllocator, message);
	}

	@Override
	public Object decodeMessage(ByteBuf data) throws IOException {
		return decode(data);
	}

	private ByteBuf encode(ByteBufAllocator bufAllocator, Object obj) throws IOException {
		ByteBuf cacheBuffer = Unpooled.wrappedBuffer(NetUtils.LOCAL_BUFFER.get());
		try {
			// wrap会认为bytes中的数据都是可读的，我们需要清空这些标记。
			cacheBuffer.clear();

			ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(cacheBuffer);
			// 协议classId
			int messageId = messageMapper.getMessageId(obj.getClass());
			byteBufOutputStream.writeInt(messageId);
			// 写入序列化的内容
			JsonUtils.getMapper().writeValue((OutputStream) byteBufOutputStream, obj);
			// 写入byteBuf
			ByteBuf byteBuf = bufAllocator.buffer(cacheBuffer.readableBytes());
			byteBuf.writeBytes(cacheBuffer);
			return byteBuf;
		} finally {
			cacheBuffer.release();
		}
	}

	private Object decode(ByteBuf data) throws IOException {
		final Class<?> messageClazz = messageMapper.getMessageClazz(data.readInt());
		final ByteBufInputStream inputStream = new ByteBufInputStream(data);
		return JsonUtils.getMapper().readValue((InputStream) inputStream, messageClazz);
	}

}
