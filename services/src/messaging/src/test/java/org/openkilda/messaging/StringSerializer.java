/* Copyright 2017 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.messaging;

import static org.openkilda.messaging.Utils.MAPPER;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public interface StringSerializer extends AbstractSerializer {
    Queue<String> strings = new LinkedList<>();

    @Override
    default Object deserialize() throws IOException {
        return MAPPER.readValue(strings.poll(), Message.class);
    }

    @Override
    default void serialize(Object object) throws IOException {
        strings.add(MAPPER.writeValueAsString(object));
    }
}
