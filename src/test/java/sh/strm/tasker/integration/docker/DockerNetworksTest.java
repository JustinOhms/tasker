package sh.strm.tasker.integration.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import sh.strm.tasker.TaskConfiguration;
import sh.strm.tasker.integration.docker.DockerAllTests.CustomTestYamlInitialization;
import sh.strm.tasker.runner.DockerTaskRunner;
import sh.strm.tasker.runner.TaskExecutionResult;
import sh.strm.tasker.task.DockerTask;
import sh.strm.tasker.util.Docker;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = CustomTestYamlInitialization.class)
public class DockerNetworksTest {

	@Autowired
	private TaskConfiguration conf;

	@Autowired
	private DockerTaskRunner dockerRunner;

	@Autowired
	private Docker client;

	@Test
	public void testDockerRunContainerWithNetwork() throws Exception {
		DockerTask taskWrite = conf.getDockerTaskByName("helloWithNetwork01");

		final AtomicBoolean secondSuccess = new AtomicBoolean(false);
		Thread otherContainer = new Thread(() -> {
			DockerTask taskRead = conf.getDockerTaskByName("helloWithNetwork02");
			try {
				TaskExecutionResult resultSecond = dockerRunner.executeTask(taskRead);
				secondSuccess.set(resultSecond.isSuccessful());
			} catch (Exception e) {
				System.err.println("Error on background container: " + e.getMessage());
			}
		});
		otherContainer.start();

		Thread.sleep(5000);

		TaskExecutionResult resultFirst = dockerRunner.executeTask(taskWrite);

		assertEquals("green bar", resultFirst.getOutput());
		assertTrue(resultFirst.isSuccessful());

		otherContainer.join(60000);
		assertTrue("Check if background container was succesful", secondSuccess.get());

		client.removeNetwork("testNetwork");
	}

	@Test
	public void testDockerNetworkParseOK() throws Exception {
		DockerTask task = new DockerTask();
		task.setNetwork("ItWillWork");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDockerNetworkParseError() throws Exception {
		DockerTask task = new DockerTask();
		task.setNetwork("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDockerNetworkParseErrorNull() throws Exception {
		DockerTask task = new DockerTask();
		task.setNetwork(null);
	}

}
