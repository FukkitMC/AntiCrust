package com.github.fukkitmc.anticrust;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class AntiCrust {
	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException {
		File temp = new File("temp");
		System.out.println("Extracting resources!");
		extract(temp);
		System.out.println("Running gronk tasks!");
		gronk(temp);
		File output = new File("output");
		output.mkdirs();
		System.out.println("Moving patches");
		movePatches(temp, output);
		System.out.println("Patches made, output/patches!");
		File map = new File(temp, "map");
		map.mkdirs();
		File craftbukkit = new File(map, "craftbukkit.jar");
		System.out.println("extracting craftbukkit!");
		extractCraftbukkit(temp, craftbukkit);
		System.out.println("mapping craftbukkit!");
		mapCraftBukkit(map, temp, output, craftbukkit);
		System.out.println("cleaning up!");
		delDir(temp.toPath());
	}

	private static void mapCraftBukkit(File map, File temp, File output, File craftbukkit) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		File tinyJar = new File(map, "tiny.jar");
		download("https://maven.fabricmc.net/net/fabricmc/yarn/1.15.1%2Bbuild.37/yarn-1.15.1%2Bbuild.37-v2.jar", tinyJar);
		File yarn = new File(map, "yarn.tiny");
		extractJar(tinyJar, "mappings/mappings.tiny", yarn);
		File crusty = new File(map, "crusty.tiny");
		extractJar(new File(temp, ".gradle/1.15.1.jar"), "mappings/mappings.tiny", crusty);
		File remapper = new File(map, "remapper.jar");
		download("https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.2.1.63/tiny-remapper-0.2.1.63-fat.jar", remapper);

		File inter = new File(map, "int.jar");
		runJar(remapper, craftbukkit.getAbsolutePath(), inter.getAbsolutePath(), crusty.getAbsolutePath(), "named", "intermediary");
		File decrusted = new File(output, "decrusted.jar");
		runJar(remapper, inter.getAbsolutePath(), decrusted.getAbsolutePath(), yarn.getAbsolutePath(), "intermediary", "named");
	}

	private static void movePatches(File temp, File output) throws IOException {
		File patches = new File(output, "patches");
		Files.move(new File(temp, "patches").toPath(), patches.toPath(), REPLACE_EXISTING);
	}


	private static void extractCraftbukkit(File temp, File craftbukkit) throws IOException {
		if (!craftbukkit.exists())
			Files.move(new File(temp, "build/buildtools/CraftBukkit/target/original-craftbukkit-1.15.1-R0.1-SNAPSHOT.jar").toPath(), craftbukkit.toPath(), REPLACE_EXISTING);
		deleteEntry(craftbukkit, "net", "com");
	}

	private static void extract(File temp) throws IOException {
		migrateResource("build.gradle", temp);
		migrateResource("gradle.properties", temp);
		migrateResource("settings.gradle", temp);
		migrateResource("diff/diff.exe", temp);
	}

	private static void deleteEntry(File jar, String... del) throws IOException {
		Map<String, String> properties = new HashMap<>();
		properties.put("create", "false");
		properties.put("encoding", "UTF-8");
		System.out.println(URI.create("jar:" + jar.toURI().toString()));
		try (FileSystem system = FileSystems.newFileSystem(URI.create("jar:" + jar.toURI().toString()), properties)) {
			for (String s : del) {
				Path entry = system.getPath(s);
				delDir(entry);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void delDir(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries)
					delDir(entry);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void gronk(File temp) throws IOException, InterruptedException {
		System.out.println("Executing: downloadARR");
		execute(temp, "gradle.bat", "downloadARR");
		System.out.println("Executing: runARR");
		execute(temp, "gradle.bat", "runARR");
		System.out.println("Executing: decrustVanilla");
		execute(temp, "gradle.bat", "decrustVanilla");
		System.out.println("Executing: decrustCraftBukkit");
		execute(temp, "gradle.bat", "decrustCraftBukkit");
		System.out.println("Executing: createPatches");
		execute(temp, "gradle.bat", "createPatches");
		System.out.println("Executing: parsePatches");
		execute(temp, "gradle.bat", "parsePatches");
	}

	private static void runJar(File jar, String... args) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		JarFile file = new JarFile(jar);
		String mainClazz = file.getManifest().getMainAttributes().getValue("Main-Class");
		URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()});
		Class<?> mainClass = loader.loadClass(mainClazz);
		Method main = mainClass.getDeclaredMethod("main", String[].class);
		main.invoke(null, (Object) args);
	}

	private static void download(String url, File file) throws IOException {
		URL u = new URL(url);
		copy(u.openStream(), new FileOutputStream(file));
	}

	private static void extractJar(File jar, String entry, File output) throws IOException {
		JarFile file = new JarFile(jar);
		copy(file.getInputStream(file.getEntry(entry)), new FileOutputStream(output));
	}

	private static Process execute(File working, String... args) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(args);
		builder.directory(working);
		Process process = builder.start();
		new Thread(new PrintingRunnable(process)).start();
		process.waitFor();
		return process;
	}

	private static void migrateResource(String path, File output) throws IOException {
		System.out.println("Extracting: " + path);
		File file = new File(output, path);
		file.getParentFile().mkdirs();
		if (!file.exists()) copy(AntiCrust.class.getResourceAsStream('/' + path), new FileOutputStream(file));
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);
		output.close();
		input.close();
	}
}
