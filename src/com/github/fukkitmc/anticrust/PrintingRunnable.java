package com.github.fukkitmc.anticrust;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PrintingRunnable implements Runnable {

	private Process process;

	public PrintingRunnable(Process process) {
		this.process = process;
	}

	@Override
	public void run() {
		try {
			BufferedReader stream = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			while (this.process.isAlive()) {
				stream.lines().filter(s -> !s.startsWith("Process finished with exit code")).forEach(System.out::println);
				AntiCrust.copy(this.process.getErrorStream(), System.err);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
