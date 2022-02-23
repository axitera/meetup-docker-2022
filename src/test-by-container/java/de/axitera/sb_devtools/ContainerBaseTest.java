package de.axitera.sb_devtools;

import de.axitera.sb_devtools.ContainerBaseTest.DockerPostgreDataSourceInitializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = DockerPostgreDataSourceInitializer.class)
@AutoConfigureMockMvc
@Testcontainers
public class ContainerBaseTest {

	public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:9.4");
	public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));


	static {
		System.out.println(postgreDBContainer.getDockerClient().versionCmd().exec());
		postgreDBContainer.start();
		kafka.start();
	}

	public static class DockerPostgreDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
					applicationContext,
					"spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
					"spring.datasource.username=" + postgreDBContainer.getUsername(),
					"spring.datasource.password=" + postgreDBContainer.getPassword(),
					"spring.kafka.bootstrap-servers="+kafka.getBootstrapServers()
			);
		}
	}
}
