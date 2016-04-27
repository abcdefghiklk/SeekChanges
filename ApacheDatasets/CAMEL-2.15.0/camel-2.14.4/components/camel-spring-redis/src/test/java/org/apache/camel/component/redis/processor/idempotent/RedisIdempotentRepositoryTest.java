/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.redis.processor.idempotent;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisIdempotentRepositoryTest {
    private static final String REPOSITORY = "testRepository";
    private static final String KEY = "KEY";
    private RedisTemplate redisTemplate;
    private SetOperations setOperations;
    private RedisIdempotentRepository idempotentRepository;

    @Before
    public void setUp() throws Exception {
        redisTemplate = mock(RedisTemplate.class);
        setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        idempotentRepository = RedisIdempotentRepository.redisIdempotentRepository(redisTemplate, REPOSITORY);
    }

    @Test
    public void shouldAddKey() {
        idempotentRepository.add(KEY);
        verify(setOperations).add(REPOSITORY, KEY);
    }

    @Test
    public void shoulCheckForMembers() {
        idempotentRepository.contains(KEY);
        verify(setOperations).isMember(REPOSITORY, KEY);
    }

    @Test
    public void shouldRemoveKey() {
        idempotentRepository.remove(KEY);
        verify(setOperations).remove(REPOSITORY, KEY);
    }

    @Test
    public void shouldReturnProcessorName() {
        String processorName = idempotentRepository.getProcessorName();
        assertEquals(REPOSITORY, processorName);
    }
}
