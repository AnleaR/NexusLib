package su.nexus.lib.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorsUtil {

	private static final Pattern hexPattern;
	private static final Pattern gradientPattern;
	private static final Pattern rainbowPattern;

	static {
		hexPattern = Pattern.compile("&#[a-fA-F0-9]{6}");
		gradientPattern = Pattern.compile("&#g.[a-fA-F0-9]{6}.[a-fA-F0-9]{6}");
		rainbowPattern = Pattern.compile("&#r");
	}

	public static String color(String message) {
		message = replaceSpecialSymbols(message);
		for (Matcher matcher = rainbowPattern.matcher(message); matcher.find(); matcher = rainbowPattern.matcher(message)) {
			int start = matcher.start();
			int end = matcher.end();
			String text = message.substring(end);
			int next = message.length();
			String style = "";
			while (text.length() >= 2) {
				if (text.charAt(0) == '&') {
					if (isStyle(text.substring(0, 2))) {
						style = style + text.substring(0, 2);
						int nextSymbol = text.replaceFirst("&", " ").indexOf("&");
						if (nextSymbol == -1) {
							text = message.substring(end + style.length());
						} else {
							text = text.substring(2);
						}
					} else {
						break;
					}
				} else {
					if (text.contains("&")) {
						next = end + text.indexOf("&");
						text = message.substring(end, next);
					}
					break;
				}
			}
			message = message.replace(message.substring(start, next), rainbow(text, style));
		}
		for (Matcher matcher = gradientPattern.matcher(message); matcher.find(); matcher = gradientPattern.matcher(message)) {
			int start = matcher.start();
			int end = matcher.end();
			String color = message.substring(start, end);
			String text = message.substring(end);
			int next = message.length();
			String style = "";
			while (text.length() >= 2) {
				if (text.charAt(0) == '&') {
					if (isStyle(text.substring(0, 2))) {
						style = style + text.substring(0, 2);
						int nextSymbol = text.replaceFirst("&", " ").indexOf("&");
						if (nextSymbol == -1) {
							text = message.substring(end + style.length());
						} else {
							text = text.substring(2);
						}
					} else {
						break;
					}
				} else {
					if (text.contains("&")) {
						next = end + text.indexOf("&");
						text = message.substring(end, next);
					}
					break;
				}
			}
			String fromHex = color.substring(4, 10);
			String toHex = color.substring(11, 17);
			message = message.replace(message.substring(start, next), gradient(fromHex, toHex, text, style));
		}
		for (Matcher matcher = hexPattern.matcher(message); matcher.find(); matcher = hexPattern.matcher(message)) {
			String color = message.substring(matcher.start(), matcher.end());
			message = message.replace(color, ChatColor.of(color.substring(1)) + "");
		}
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public static String withOutColors(String message) {
		for (Matcher matcher = rainbowPattern.matcher(message); matcher.find(); matcher = rainbowPattern.matcher(message)) {
			int start = matcher.start();
			int end = matcher.end();
			message = message.replace(message.substring(start, end), "");
		}
		for (Matcher matcher = gradientPattern.matcher(message); matcher.find(); matcher = gradientPattern.matcher(message)) {
			int start = matcher.start();
			int end = matcher.end();
			message = message.replace(message.substring(start, end), "");
		}
		for (Matcher matcher = hexPattern.matcher(message); matcher.find(); matcher = hexPattern.matcher(message)) {
			int start = matcher.start();
			int end = matcher.end();
			message = message.replace(message.substring(start, end), "");
		}
		while (message.contains("&")) {
			int i = message.indexOf("&");
			message = message.replace(message.substring(i, i + 2), "");
		}
		return message;
	}

	public static Color hexToRgb(String hex) throws IndexOutOfBoundsException {
		return new Color(
				Integer.valueOf(hex.substring(0, 2), 16),
				Integer.valueOf(hex.substring(2, 4), 16),
				Integer.valueOf(hex.substring(4, 6), 16));
	}

	public static String rgbToHex(Color rgb) throws IndexOutOfBoundsException {
		int red = rgb.getRed();
		int green = rgb.getGreen();
		int blue = rgb.getBlue();
		Color color;
		color = new Color(red, green, blue);
		StringBuilder hex = new StringBuilder(Integer.toHexString(color.getRGB() & 0xffffff));
		while(hex.length() < 6){
			hex.insert(0, "0");
		}
		return  hex.toString();
	}

	public static String gradient(String fromHEX, String toHEX, String message, String style) {
		Color from = hexToRgb(fromHEX);
		Color to = hexToRgb(toHEX);

		int length = message.length();
		if (length == 0) {
			return "";
		}
		Map<Integer, Color> rgbMap = new HashMap<>();

		int fromRed = from.getRed();
		int fromGreen = from.getGreen();
		int fromBlue = from.getBlue();

		int differenceRed = to.getRed() - fromRed;
		int differenceGreen = to.getGreen() - fromGreen;
		int differenceBlue = to.getBlue() - fromBlue;

		differenceRed = differenceRed == 0 ? 0 : differenceRed / length;
		differenceGreen = differenceGreen == 0 ? 0 : differenceGreen/ length;
		differenceBlue = differenceBlue == 0 ? 0 : differenceBlue / length;

		for (int i = 0; i < length; i++) {
			Color color = new Color(
					fromRed + (differenceRed * i),
					fromGreen + (differenceGreen * i),
					fromBlue + (differenceBlue * i));
			rgbMap.put(i, color);
		}

		Map<Integer, String> hexMap = new HashMap<>();
		for (int i = 0; i < rgbMap.size(); i++) {
			hexMap.put(i, rgbToHex(rgbMap.get(i)));
		}

		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < hexMap.size(); i++) {
			stringBuilder.append("&#").append(hexMap.get(i)).append(style).append(message.charAt(i));
		}
		return stringBuilder.toString();
	}

	public static String rainbow(String message, String style) {
		boolean endsWithSpace = false;
		StringBuilder spaces = new StringBuilder();
		if (message.endsWith(" ")) {
			endsWithSpace = true;
			spaces.append(" ");
			for (int i = 2; i < message.length(); i++) {
				if (message.substring(0, message.length() - i).endsWith(" ")) {
					spaces.append(" ");
				} else {
					break;
				}
			}
		}

		boolean spaceAdded = false;
		int length = message.length();
		if (length < 7) {
			StringBuilder messageBuilder = new StringBuilder(message);
			for (int i = (7 - length); i > 0; i--) {
				messageBuilder.append(" ");
			}
			message = messageBuilder.toString();
			spaceAdded = true;
		}

		length = message.length();
		java.util.List<String> strings = new ArrayList<>();
		int divider = 7;
		while (!message.equals("")) {
			int ratio = length / divider;
			strings.add(message.substring(0, ratio));
			message = message.replaceFirst(message.substring(0, ratio), "");
			length = message.length();
			--divider;
		}

		List<String> finalStrings = new ArrayList<>();
		if (!strings.get(0).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("fa0505", "f79c14", strings.get(0), style));
		}
		if (!strings.get(1).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("f79c14", "f4f711", strings.get(1), style));
		}
		if (!strings.get(2).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("f4f711", "24f711", strings.get(2), style));
		}
		if (!strings.get(3).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("24f711", "0afafa", strings.get(3), style));
		}
		if (!strings.get(4).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("0afafa", "284bfa", strings.get(4), style));
		}
		if (!strings.get(5).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("284bfa", "7b07f0", strings.get(5), style));
		}
		if (!strings.get(6).equals(" ") || !spaceAdded) {
			finalStrings.add(gradient("7b07f0", "fa0505", strings.get(6), style));
		}

		StringBuilder stringBuilder = new StringBuilder();
		for (String finalString : finalStrings) {
			stringBuilder.append(finalString);
		}

		String result = stringBuilder.toString().trim();
		if (!result.equals("")) {
			if (result.charAt(result.length() - 1) == ' ') {
				result = result.substring(0, result.length() - 1);
			}
		}

		return endsWithSpace ? result + spaces : result;
	}

	public static boolean isStyle(String str) {
		if (str.startsWith("&")) {
			return str.equals("&r") || str.equals("&l") || str.equals("&o") || str.equals("&n") || str.equals("&m") || str.equals("&k");
		}
		return false;
	}

	public static String replaceSpecialSymbols(String message) {
		if (message.contains(":)")) {
			message = message.replaceAll(":\\)", "☺");
		}
		if (message.contains(":(")) {
			message = message.replaceAll(":\\(", "☹");
		}
		if (message.contains("<3")) {
			message = message.replaceAll("<3", "❤");
		}
		if (message.contains("-_-")) {
			message = message.replaceAll("-_-", "ಠ_ಠ");
		}
		if (message.contains("*-*")) {
			message = message.replaceAll("\\*-\\*", "✞");
		}
		if (message.contains("^-^")) {
			message = message.replaceAll("\\^-\\^", "✿");
		}
		return message;
	}

	public static int getMessageLengthWithoutColor(String message) {
		int length = 0;

		Matcher rainbowMatcher = rainbowPattern.matcher(message);
		Matcher gradientMatcher = gradientPattern.matcher(message);
		Matcher hexMatcher = hexPattern.matcher(message);

		while (rainbowMatcher.find()) {
			final String color = message.substring(rainbowMatcher.start(), rainbowMatcher.end());
			message = message.replace(color, "");
			rainbowMatcher = rainbowPattern.matcher(message);
			length = message.length();
		}

		while (gradientMatcher.find()) {
			final String color = message.substring(gradientMatcher.start(), gradientMatcher.end());
			message = message.replace(color, "");
			gradientMatcher = gradientPattern.matcher(message);
			length = message.length();
		}

		while (hexMatcher.find()) {
			final String color = message.substring(hexMatcher.start(), hexMatcher.end());
			message = message.replace(color, "");
			hexMatcher = hexPattern.matcher(message);
			length = message.length();
		}

		while (message.contains("&")) {
			final String color = message.substring(message.indexOf("&"), message.indexOf("&") + 1);
			message = message.replace(color, "");
			length = message.length();
		}

		return length;
	}
}