package nl.tudelft.ewi.devhub.webtests.views;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import nl.tudelft.ewi.git.models.EntryType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

public class FolderView extends ProjectInCommitView {

	private static final By TABLE_FILES = By.xpath(".//table[@class='table files']");

	public FolderView(WebDriver driver) {
		super(driver);
	}

	/**
	 * @return get the folder path
	 */
	public String getPath() {
		return getDriver().findElement(By.cssSelector("div.header > h5")).getText().replace(" /", "/").replace("/ ", "/");
	}

	/**
	 * @return A {@code Map} containing the entry names and types
	 */
	public Map<String, EntryType> getDirectoryEntries() {
		ImmutableMap.Builder<String, EntryType> builder = ImmutableMap.builder();
		for (DirectoryElement entry : getDirectoryElements()) {
			builder.put(entry.getName(), entry.getType());
		}
		return builder.build();
	}

	/**
	 * @return A {@code List} containing the {@link DirectoryElement}s
	 */
	public List<DirectoryElement> getDirectoryElements() {
		ImmutableList.Builder<DirectoryElement> result = ImmutableList.builder();
		for (WebElement element : getDriver().findElement(TABLE_FILES).findElements(By.tagName("td"))) {
			DirectoryElement entry = new DirectoryElement(element);
			result.add(entry);
		}
		return result.build();
	}

	@Data
	public class DirectoryElement {

		private final WebElement element;
		private final EntryType type;
		private final String name;

		public DirectoryElement(WebElement rowElement) {
			this.element = rowElement.findElement(By.tagName("a"));
			this.name = rowElement.getText();
			String iconStyles = rowElement.findElement(By.tagName("i")).getAttribute("class");
			this.type = iconStyles.contains("glyphicon-folder-open") ? EntryType.FOLDER
				: iconStyles.contains("glyphicon-file") ? EntryType.TEXT
					: EntryType.BINARY;
		}

		public TextFileInCommitView click() {
			element.click();
			return new TextFileInCommitView(getDriver());
		}

	}

}
