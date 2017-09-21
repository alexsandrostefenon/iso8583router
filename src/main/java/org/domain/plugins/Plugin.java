package org.domain.plugins;

public interface Plugin {
	void start();
	void stop();
	long execute(Object data);
}
