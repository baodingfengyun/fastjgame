package com.wjybxx.fastjgame.test;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.wjybxx.fastjgame.protobuffer.p_center_scene;
import com.wjybxx.fastjgame.protobuffer.p_center_scene.p_center_cross_scene_hello;

/**
 * descriptor无法获取到注释
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/21 23:00
 * github - https://github.com/hl845740757
 */
public class DescriptorTest {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        Descriptors.Descriptor descriptor = p_center_cross_scene_hello.getDescriptor();
        System.out.println(descriptor);
    }
}
