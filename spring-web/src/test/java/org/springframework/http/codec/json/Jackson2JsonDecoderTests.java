/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.json;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import org.junit.Test;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.codec.json.Jackson2JsonDecoder.*;
import static org.springframework.http.codec.json.JacksonViewBean.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.Pojo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Jackson2JsonDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class Jackson2JsonDecoderTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void canDecode() {
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		assertTrue(decoder.canDecode(type, APPLICATION_JSON));
		assertTrue(decoder.canDecode(type, null));
		assertFalse(decoder.canDecode(type, APPLICATION_XML));
	}

	@Test
	public void decodePojo() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("foofoo", "barbar"))
				.verifyComplete();
	}

	@Test
	public void decodePojoWithError() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\":}"));
		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(flux).verifyError(CodecException.class);
	}

	@Test
	public void decodeToList() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);
		Mono<Object> mono = new Jackson2JsonDecoder().decodeToMono(source, elementType,
				null, emptyMap());

		StepVerifier.create(mono)
				.expectNext(asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeToFlux() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("f1", "b1"))
				.expectNext(new Pojo("f2", "b2"))
				.verifyComplete();
	}

	@Test
	public void fieldLevelJsonView() throws Exception {
		Flux<DataBuffer> source = Flux.just(
				stringBuffer("{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);
		Flux<JacksonViewBean> flux = new Jackson2JsonDecoder()
				.decode(source, elementType, null, hints).cast(JacksonViewBean.class);

		StepVerifier.create(flux)
				.consumeNextWith(b -> {
					assertTrue(b.getWithView1().equals("with"));
					assertNull(b.getWithView2());
					assertNull(b.getWithoutView());
				})
				.verifyComplete();
	}

	@Test
	public void classLevelJsonView() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);
		Flux<JacksonViewBean> flux = new Jackson2JsonDecoder()
				.decode(source, elementType, null, hints).cast(JacksonViewBean.class);

		StepVerifier.create(flux)
				.consumeNextWith(b -> {
					assertNull(b.getWithView1());
					assertNull(b.getWithView2());
					assertTrue(b.getWithoutView().equals("without"));
				})
				.verifyComplete();
	}

	@Test
	public void decodeEmptyBodyToMono() throws Exception {
		Flux<DataBuffer> source = Flux.empty();
		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Mono<Object> mono = new Jackson2JsonDecoder().decodeToMono(source, elementType,
				null, emptyMap());

		StepVerifier.create(mono)
				.expectNextCount(0)
				.verifyComplete();
	}

}
