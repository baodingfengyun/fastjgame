/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.net.example;

import com.wjybxx.fastjgame.net.binary.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/26
 */
public class EntityExample {


    public static void main(String[] args) throws Exception {
        BinaryProtocolCodec binaryCodec = ExampleConstants.binaryCodec;

        final ChildBean childBean = new ChildBean(new Child1(123456, "wjybxx"), false);
        System.out.println(childBean);

        childBean.parent.doAction();


        final ChildBean clonedObject = (ChildBean) binaryCodec.cloneObject(childBean);
        clonedObject.parent.doAction();
    }

    @SerializableClass
    public static abstract class Parent {

        @SerializableField
        private int id;

        @SerializableField
        private String name;

        protected Parent(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public abstract void doAction();

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("name", name)
                    .toString();
        }
    }

    public static class Child1 extends Parent {

        private Child1() {
            this(0, "");
        }

        public Child1(int id, String name) {
            super(id, name);
        }

        @Override
        public void doAction() {
            System.out.println("child1 action");
        }
    }

    public static class Child2 extends Parent {

        private Child2() {
            this(0, "");
        }

        public Child2(int id, String name) {
            super(id, name);
        }

        @Override
        public void doAction() {
            System.out.println("child2 action");
        }
    }

    public static class ChildBean {

        private Parent parent;
        private boolean decodeAsChild1;

        private ChildBean(Parent parent, boolean decodeAsChild1) {
            this.parent = parent;
            this.decodeAsChild1 = decodeAsChild1;
        }

        public boolean isDecodeAsChild1() {
            return decodeAsChild1;
        }

        public void setDecodeAsChild1(boolean decodeAsChild1) {
            this.decodeAsChild1 = decodeAsChild1;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("parent", parent)
                    .append("decodeAsChild1", decodeAsChild1)
                    .toString();
        }
    }


    @SuppressWarnings("unused")
    public static class ChildBeanSerializer implements EntitySerializer<ChildBean> {

        @Override
        public Class<ChildBean> getEntityClass() {
            return ChildBean.class;
        }

        @Override
        public ChildBean readObject(EntityInputStream inputStream) throws Exception {
            final boolean isChild1 = inputStream.readBoolean();
            final Parent parent;
            if (isChild1) {
                parent = inputStream.readEntity(Child1::new, Parent.class);
            } else {
                parent = inputStream.readEntity(Child2::new, Parent.class);
            }
            return new ChildBean(parent, isChild1);
        }

        @Override
        public void writeObject(ChildBean instance, EntityOutputStream outputStream) throws Exception {
            outputStream.writeBoolean(instance.isDecodeAsChild1());
            outputStream.writeEntity(instance.parent, Parent.class);
        }
    }
}
