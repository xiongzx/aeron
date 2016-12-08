/*
 * Copyright 2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.driver.media;

import io.aeron.driver.Configuration;
import io.aeron.protocol.HeaderFlyweight;
import io.aeron.protocol.NakFlyweight;
import io.aeron.protocol.StatusMessageFlyweight;
import org.agrona.BitUtil;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.agrona.BitUtil.CACHE_LINE_LENGTH;

public class ReceiveChannelEndpointThreadLocals
{
    private final ByteBuffer smBuffer;
    private final StatusMessageFlyweight statusMessageFlyweight;
    private final ByteBuffer nakBuffer;
    private final NakFlyweight nakFlyweight;

    public ReceiveChannelEndpointThreadLocals()
    {
        final byte[] applicationSpecificFeedback = Configuration.SM_APPLICATION_SPECIFIC_FEEDBACK;
        final int smLength = StatusMessageFlyweight.HEADER_LENGTH + applicationSpecificFeedback.length;
        final int bufferLength =
            BitUtil.align(smLength, CACHE_LINE_LENGTH) +
            BitUtil.align(NakFlyweight.HEADER_LENGTH, CACHE_LINE_LENGTH);

        final UUID uuid = UUID.randomUUID();
        final long receiverId = (uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());

        final ByteBuffer byteBuffer = NetworkUtil.allocateDirectAlignedAndPadded(bufferLength, CACHE_LINE_LENGTH);

        byteBuffer.limit(smLength);
        smBuffer = byteBuffer.slice();
        statusMessageFlyweight = new StatusMessageFlyweight(smBuffer);

        final int nakMessageOffset = BitUtil.align(smLength, 32);
        byteBuffer.limit(nakMessageOffset + NakFlyweight.HEADER_LENGTH).position(nakMessageOffset);
        nakBuffer = byteBuffer.slice();
        nakFlyweight = new NakFlyweight(nakBuffer);

        statusMessageFlyweight
            .applicationSpecificFeedback(applicationSpecificFeedback, 0, applicationSpecificFeedback.length)
            .receiverId(receiverId)
            .version(HeaderFlyweight.CURRENT_VERSION)
            .headerType(HeaderFlyweight.HDR_TYPE_SM)
            .frameLength(StatusMessageFlyweight.HEADER_LENGTH + applicationSpecificFeedback.length);

        nakFlyweight
            .version(HeaderFlyweight.CURRENT_VERSION)
            .headerType(HeaderFlyweight.HDR_TYPE_NAK)
            .frameLength(NakFlyweight.HEADER_LENGTH);
    }

    public ByteBuffer smBuffer()
    {
        return smBuffer;
    }

    public StatusMessageFlyweight statusMessageFlyweight()
    {
        return statusMessageFlyweight;
    }

    public ByteBuffer nakBuffer()
    {
        return nakBuffer;
    }

    public NakFlyweight nakFlyweight()
    {
        return nakFlyweight;
    }
}
