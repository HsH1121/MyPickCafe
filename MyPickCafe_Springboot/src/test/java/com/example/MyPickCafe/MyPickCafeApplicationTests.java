package com.example.MyPickCafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인한다.
 *
 * <p>test 프로파일(H2 인메모리)을 쓰므로 로컬에 PostgreSQL/Docker가
 * 없어도 실행된다.
 */
@SpringBootTest
@ActiveProfiles("test")
class MyPickCafeApplicationTests {

	@Test
	void contextLoads() {
	}

}
