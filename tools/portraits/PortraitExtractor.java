import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class PortraitExtractor {
	public static void main(String[] args) throws Exception {
		List<String> files = readFiles();
		List<List<Extraction>> data = readData();
		int ext = 0;
		new File("./portraits_out/").mkdir();
		System.out.println("Beginning image extraction.");
		for (int i = 0; i < files.size(); i++) {
			String file = files.get(i);
			if (data.get(i).isEmpty()) continue;
			try {
				BufferedImage src = readImage(file);
				for (Extraction e : data.get(i)) {
					System.out.print("\rProcessing " + i + " of " + files.size() + " (extracting " + ext + ")");
					extract(src, e, new File("./portraits_out/" + ext++ + ".png"));
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println();
			}
		}
	}

	private static class Extraction {
		int x;
		int y;
		int w;
	}

	public static List<String> readFiles() throws IOException {
		String dataJs = Files.readString(Paths.get("files.js"), StandardCharsets.UTF_8);
		return new Gson().fromJson(dataJs.replace(";", "").replace("let files = ", ""), new TypeToken<List<String>>(){}.getType());
	}

	public static List<List<Extraction>> readData() throws IOException {
		String dataJs = Files.readString(Paths.get("data.js"), StandardCharsets.UTF_8);
		return new Gson().fromJson(dataJs.replace(";", "").replace("let data = ", ""), new TypeToken<List<List<Extraction>>>(){}.getType());
	}

	public static BufferedImage readImage(String location) throws IOException {
		try {
			return ImageIO.read(new File(location));
		} catch (IOException e) {
			throw new IOException("Failure to read " + location, e);
		}
	}

	public static void extract(BufferedImage src, Extraction e, File outFile) throws IOException {
		double scale = 384.0 / e.w;
		if (scale > 1) scale = 1;
		int height = Math.min((int)(1080 / scale), src.getHeight() - e.y);
		BufferedImage intm = new BufferedImage(e.w, height, BufferedImage.TYPE_INT_ARGB);
		int[] raster = src.getRGB(e.x, e.y, e.w, height, null, 0, e.w);
		// TODO: could implement some auto-contrast on raster here.
		intm.setRGB(0, 0, e.w, height, raster, 0, e.w);
		BufferedImage out = new BufferedImage((int)(e.w * scale), (int)(height * scale), BufferedImage.TYPE_INT_ARGB);
		out.createGraphics().drawImage(intm.getScaledInstance((int)(e.w * scale), (int)(height * scale), Image.SCALE_SMOOTH), 0, 0, null);
		ImageIO.write(out, "png", outFile);
	}
}
