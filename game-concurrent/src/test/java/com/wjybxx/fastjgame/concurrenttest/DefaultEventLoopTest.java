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

package com.wjybxx.fastjgame.concurrenttest;

import com.wjybxx.fastjgame.concurrent.DefaultEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/23
 * github - https://github.com/hl845740757
 */
public class DefaultEventLoopTest {

	public static void main(String[] args) {
		DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(2, new DefaultThreadFactory("test"));
		ListenableFuture<String> future = defaultEventLoopGroup.submit(() -> {
			System.out.println("before task1 return.");
			Thread.sleep(200);
			return "-hello world.";
		});
		ListenableFuture<String> future2 = defaultEventLoopGroup.submit(() -> {
			System.out.println("before task2  return.");
			Thread.sleep(200);
			return "-java";
		});


		System.out.println("before await");
		future.awaitUninterruptibly();
		future2.awaitUninterruptibly();

		System.out.println(future.tryGet());
		System.out.println(future2.tryGet());

		defaultEventLoopGroup.shutdown();
	}
}
