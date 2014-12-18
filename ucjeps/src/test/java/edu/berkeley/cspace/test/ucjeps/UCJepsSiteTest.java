package edu.berkeley.cspace.test.ucjeps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author ray
 *
 */
public class UCJepsSiteTest {
	public static final String HOST = "cspace";
	public static final String TENANT = "ucjeps";
	public static final String USERNAME = "admin@ucjeps.cspace.berkeley.edu";
	public static final String PASSWORD = "Administrator";
	public static final String TENANT_BASE_URL = "http://" + HOST + ":8180/collectionspace/ui/" + TENANT + "/html/";
	public static final long TIMEOUT = 3;
	
	WebDriver driver = new FirefoxDriver();
	
	@BeforeClass
	public void setUp() {	
		driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
	}
	
	/**
	 * Tests logging in to the tenant.
	 * <ul>
	 * <li>Login should succeed</li>
	 * </ul>
	 */
	@Test
	public void testLogin() {
		driver.get(TENANT_BASE_URL + "index.html");

		driver.findElement(By.className("csc-login-userId")).sendKeys(USERNAME);
		driver.findElement(By.className("csc-login-password")).sendKeys(PASSWORD);
		driver.findElement(By.className("csc-login-button")).click();

		String errorMessage = null;
		
		try {
			WebElement errorElement = driver.findElement(By.className("cs-message-error"));
			WebElement messageElement = errorElement.findElement(By.id("message"));
			
			errorMessage = messageElement.getText();
		}
		catch(NoSuchElementException e) {}
		
		Assert.assertNull(errorMessage, "Login failed");
	}
	
	/**
	 * Tests the login landing page.
	 * <ul>
	 * <li>Login should land on the create new page</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testLandingPage() {
		Assert.assertEquals(driver.getTitle(), "CollectionSpace - Create New Record", "Login did not land on the create new page");		
	}
	
	/**
	 * Tests the create new page.
	 * <ul>
	 * <li>The acquisition and object exit procedures should be removed</li>
	 * <li>TODO: Additional cataloging templates should be removed</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testCreateNew() {
		driver.get(TENANT_BASE_URL + "createnew.html");

		WebElement acquisitionButton = null;
		
		try {
			acquisitionButton = driver.findElement(By.cssSelector("input[value=acquisition]"));
		}
		catch(NoSuchElementException e) {}
		
		Assert.assertNull(acquisitionButton, "Acquisition procedure has not been removed");
		
		WebElement objectExitButton = null;
		
		try {
			objectExitButton = driver.findElement(By.cssSelector("input[value=objectexit]"));
		}
		catch(NoSuchElementException e) {}
		
		Assert.assertNull(objectExitButton, "Object exit procedure has not been removed");
	}

	/**
	 * Tests the cataloging record editor.
	 * <ul>
	 * <li>The following sections should be removed:
	 *     <ul>
	 *     <li>Object Production Information</li>
	 *     <li>Object Owner's Contribution Information</li>
	 *     <li>Object Viewer's Contribution Information</li>
	 *     </ul>
	 * </li>
	 * <li>The name of the Object Collection Information section should be changed to Field Collection Information</li>
	 * <li>Field collection date should be a structured date</li>
	 * <li>TODO: Determination History/Date should be a structured date</li>
	 * <li>TODO: Autocomplete search should return 60 records</li>
	 * <li>TODO: The Field Collection Source field should be removed</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testCatalogingRecordEditor() {
		driver.get(TENANT_BASE_URL + "cataloging.html");
		
		String labelText = driver.findElement(By.className("csc-collection-object-objectCollectionInformation-label")).getText();
		Assert.assertEquals(labelText, "Field Collection Information", "Incorrect section label");
		
		WebElement fieldCollectionDateElement = driver.findElement(By.className("csc-collection-object-fieldCollectionDate"));
		Assert.assertTrue(fieldCollectionDateElement.getAttribute("class").contains("cs-structuredDate-input"), "fieldCollectionDate is not a structured date");

		String[] removedSectionLabels = {
			"csc-collection-object-objectProductionInformation-label",
			"csc-collection-object-objectOwnerContributionInformation-label",
			"csc-collection-object-objectViewerContributionInformation-label"
		};
		
		for (String className : removedSectionLabels) {
			WebElement element = null;
			
			try {
				element = driver.findElement(By.className(className));
			}
			catch (NoSuchElementException e) {}
			
			Assert.assertNull(element, "Section not removed");
		}
	}

	/**
	 * Tests saving a cataloging record.
	 * <ul>
	 * <li>The record should save without error</li>
	 * <li>The Determination History/Name field (from the naturalhistory domain extension) should be saved</li>
	 * <li>The Handwritten label field (from the ucjeps local extension) should be saved</li>
	 * <li>TODO: The title should contain the primary Determination History/Name</li>
	 * <li>The Sheet field (from the naturalhistory domain extension) should be saved</li>
	 * <li>The Cultivated field (from the naturalhistory domain extension) should be saved</li>
	 * <li>TODO: The Determination History/Date field (from the naturalhistory domain extension) should be saved</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testSaveCatalogingRecord() {
		driver.get(TENANT_BASE_URL + "cataloging.html");
		
		String taxonValue = "Ulva compressa";
		String handwrittenValue = "yes";
		String sheetValue = "5";
		
		driver.findElement(By.className("csc-object-identification-object-number")).sendKeys(getTimestamp());
		fillAutocomplete("csc-taxonomic-identification-taxon", taxonValue);		
		driver.findElement(By.className("csc-collection-object-handwritten")).findElement(By.cssSelector("option[value=" + handwrittenValue + "]")).click();
		driver.findElement(By.className("csc-collection-object-objectCountNumber")).sendKeys(sheetValue);
		driver.findElement(By.className("csc-collection-object-cultivated")).click();

		driver.findElement(By.className("csc-save")).click();

		WebElement message = driver.findElement(By.className("csc-messageBar-message"));
		String messageText = message.getText();
		Assert.assertTrue(messageText.contains("success"), "Save was not successful ('" + messageText + "')");
		
		String savedTaxonValue = getAutocompleteValue("csc-taxonomic-identification-taxon");
		Assert.assertEquals(savedTaxonValue, taxonValue, "Determination History/Name was not saved correctly");
		
		String savedHandwrittenValue = driver.findElement(By.className("csc-collection-object-handwritten")).getAttribute("value");
		Assert.assertEquals(savedHandwrittenValue, handwrittenValue, "Handwritten label was not saved correctly");
		
		String savedSheetValue = driver.findElement(By.className("csc-collection-object-objectCountNumber")).getAttribute("value");
		Assert.assertEquals(savedSheetValue, sheetValue, "Sheet was not saved correctly");
		
		String cultivatedChecked = driver.findElement(By.className("csc-collection-object-cultivated")).getAttribute("checked");
		Assert.assertNotNull(cultivatedChecked, "Cultivated was not saved correctly");
	}
	
	/**
	 * Tests saving a media record.
	 * <ul>
	 * <li>TODO: The record should save without error</li>
	 * <li>TODO: The Scientific name field (from the ucjeps local extension) should be saved</li>
	 * <li>TODO: The Handwritten label field (from the ucjeps local extension) should be saved</li>
	 * <li>TODO: The title should contain the Scientific name</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testSaveMediaRecord() {
	}

	/**
	 * TODO: Tests media record sticky fields
	 * <ul>
	 * <li>TODO: The Scientific name field should be sticky</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin", "testSaveMediaRecord" })
	public void testMediaRecordEditorStickyField() {
	}

	/**
	 * Tests creating related procedures.
	 * <ul>
	 * <li>TODO: A cataloging record can be related to a media record, and appears in the sidebar after a reload</li>
	 * <li>TODO: A media record can be related to a cataloging record, and appears in the sidebar after a reload</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testRelatedProcedures() {
	}

	/**
	 * Tests the find/edit page.
	 * <ul>
	 * <li>TODO: The default search page size should be 50</li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testFindEdit() {
	}

	/**
	 * Tests the advanced search form for cataloging.
	 * <ul>
	 * <li>TODO: The following fields should be added:
	 *     <ul>
	 *     <li>TODO: Determination History/Name</li>
	 *     <li>TODO: Kind of Type</li>
	 *     <li>TODO: Field collection collector</li>
	 *     <li>TODO: Field collection location verbatim</li>   
	 *     <li>TODO: Field location county</li>   
	 *     <li>TODO: Field location state</li>   
	 *     <li>TODO: Field location country</li>   
	 *     </ul>
	 * </li>
	 * <li>TODO: The following fields should be removed:
	 *     <ul>
	 *     <li>TODO: Object Name</li>
	 *     <li>TODO: Responsible Department</li>
	 *     <li>TODO: Title</li>
	 *     <li>TODO: Copy Number</li>
	 *     <li>TODO: Edition Number</li>
	 *     <li>TODO: Material</li>
	 *     <li>TODO: Object Status</li>
	 *     <li>TODO: Style</li>
	 *     <li>TODO: Technical Attribute</li>
	 *     <li>TODO: Object Component Name</li>
	 *     <li>TODO: Production organization</li>
	 *     <li>TODO: Production people</li>
	 *     <li>TODO: Production person</li>
	 *     <li>TODO: Production place</li>
	 *     <li>TODO: Technique</li>
	 *     <li>TODO: Field collection event name</li>
	 *     </ul>
	 * </li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testCatalogingAdvancedSearch() {
		
	}

	/**
	 * Tests the advanced search form for media handling.
	 * <ul>
	 * <li>TODO: The following fields should be added:
	 *     <ul>
	 *     <li>TODO: Scientific name</li>
	 *     <li>TODO: Post to public</li>
	 *     <li>TODO: Handwritten label</li>
	 *     </ul>
	 * </li>
	 * </ul>
	 */
	@Test(dependsOnMethods = { "testLogin" })
	public void testMediaAdvancedSearch() {
		
	}

	private void fillAutocomplete(String className, String value) {
		getAutocompleteInput(className).sendKeys(value);
		
		WebElement popup = driver.findElement(By.className("cs-autocomplete-popup"));
		WebElement matchesPanel = popup.findElement(By.className("csc-autocomplete-Matches"));
		WebElement firstMatchItem = null;
		
		try {
			firstMatchItem = matchesPanel.findElement(By.tagName("li"));
		}
		catch (NoSuchElementException e) {}
		
		if (firstMatchItem != null) {
			firstMatchItem.click();			
		}
		else {
			WebElement addToPanel = popup.findElement(By.className("csc-autocomplete-addToPanel"));
			WebElement firstAuthorityListItem = addToPanel.findElement(By.tagName("li"));
			
			firstAuthorityListItem.click();
		}
	}
	
	private String getAutocompleteValue(String className) {
		return getAutocompleteInput(className).getAttribute("value");
	}
	
	private WebElement getAutocompleteInput(String className) {
		return driver.findElement(By.cssSelector("." + className + " + .cs-autocomplete-input"));
	}
	
	private String getTimestamp() {
		SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
		
		return formatter.format(new Date());
	}
}
