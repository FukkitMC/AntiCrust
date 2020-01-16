package net.devtech.anticrust;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AntiCruster {
	public static void main(String[] args) throws IOException {
		File patch = new File(args[0]);
		File folder = new File(args[1]);
		folder.mkdirs();
		BufferedReader reader = new BufferedReader(new FileReader(patch));
		Queue<String> stack = new LinkedList<>();
		reader.lines().forEach(stack::add);

		List<String> unique = new ArrayList<>();
		while (!stack.isEmpty()) {
			String diffLine = stack.poll();
			if (diffLine.startsWith("diff")) {
				String[] paths = diffLine.split("\\\\");
				String className = paths[paths.length - 1];
				File clas = new File(folder, className.replace(".java", ".patch"));
				BufferedWriter writer = new BufferedWriter(new FileWriter(clas));

				while (!stack.isEmpty() && !stack.peek().startsWith("diff")) {
					String peek = stack.poll();
					if (peek.startsWith("Only in")) {
						unique.add(peek);
						continue;
					}
					writer.write(peek);
					writer.write('\n');
				}
				writer.close();
			}
		}

		File uniques = new File(folder, "uniques.txt");
		PrintStream stream = new PrintStream(new FileOutputStream(uniques));
		unique.forEach(stream::println);
		stream.close();
	}

	private 

	private static void download(String url, File file) {
		try (InputStream fis = new URL(url).openStream(); FileOutputStream fos = new FileOutputStream(file)) {
			int len;
			byte[] buffer = new byte[4096];
			while ((len = fis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
