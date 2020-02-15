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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@SerializableClass
public class TestMsg {

    @SerializableField(number = 1)
    private long sceneId;

    @SerializableField(number = 2)
    private long factionId;

    @SerializableField(number = 3)
    private long ownerId;

    @SerializableField(number = 4)
    private boolean ownerSupportAR;

    @SerializableField(number = 5)
    private int playerNum = 1;

    @SerializableField(number = 6)
    private boolean racing = false;

    public TestMsg() {
    }

    public TestMsg(long sceneId, long factionId, long ownerId, boolean ownerSupportAR) {
        this.sceneId = sceneId;
        this.factionId = factionId;
        this.ownerId = ownerId;
        this.ownerSupportAR = ownerSupportAR;
    }

    public void setRacing(boolean isRace) {
        this.racing = isRace;
    }

    public boolean isRacing() {
        return racing;
    }

    public long getSceneId() {
        return sceneId;
    }

    public void setSceneId(long sceneId) {
        this.sceneId = sceneId;
    }

    public long getFactionId() {
        return factionId;
    }

    public void setFactionId(long factionId) {
        this.factionId = factionId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isOwnerSupportAR() {
        return ownerSupportAR;
    }

    public int getPlayerNum() {
        return playerNum < 1 ? 1 : playerNum;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public void modHumanNum(int number) {
        if (playerNum < 1) {
            playerNum = 1;
        }
        playerNum += number;
    }

    public void setOwnerSupportAR(boolean ownerSupportAR) {
        this.ownerSupportAR = ownerSupportAR;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TestMsg that = (TestMsg) o;

        return new EqualsBuilder()
                .append(sceneId, that.sceneId)
                .append(factionId, that.factionId)
                .append(ownerId, that.ownerId)
                .append(ownerSupportAR, that.ownerSupportAR)
                .append(playerNum, that.playerNum)
                .append(racing, that.racing)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(sceneId)
                .append(factionId)
                .append(ownerId)
                .append(ownerSupportAR)
                .append(playerNum)
                .append(racing)
                .toHashCode();
    }
}