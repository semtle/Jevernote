package cz.martlin.jevernote.storage.impls;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.martlin.jevernote.dataobj.storage.Item;
import cz.martlin.jevernote.dataobj.storage.Package;
import cz.martlin.jevernote.misc.JevernoteException;
import cz.martlin.jevernote.misc.Log;
import cz.martlin.jevernote.tools.TestingUtils;

public class FileSystemStorageWithIndexFileTest {
	private static final boolean WITH_ID = false;

	private static File baseDir;

	public FileSystemStorageWithIndexFileTest() {
	}

	///////////////////////////////////////////////////////////////////////////

	@Test
	public void testIndexFileItself() throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = new XXXFSwIndexFileStorageWrapper(baseDir);

		assertTrue(storage.hasIndexFile());

		storage.uninstall();

		assertFalse(storage.hasIndexFile());

		storage.install();

		assertTrue(storage.hasIndexFile());

	}

	@Test
	public void testBasicPackages() throws JevernoteException, IOException {
		final String name1 = "foo";
		final String name2 = "bar";

		createPackage(WITH_ID, name1);
		assertTrue(dirOfPack(name1).isDirectory());
		assertFalse(dirOfPack(name2).isDirectory());
		assertTrue(hasIndexPack(name1));
		assertFalse(hasIndexPack(name2));

		movePackage(WITH_ID, name1, name2);
		assertFalse(dirOfPack(name1).isDirectory());
		assertTrue(dirOfPack(name2).isDirectory());
		assertFalse(hasIndexPack(name1));
		assertTrue(hasIndexPack(name2));

		removePackage(WITH_ID, name2);
		assertFalse(dirOfPack(name2).isDirectory());
		assertFalse(dirOfPack(name2).isDirectory());
		assertFalse(hasIndexPack(name1));
		assertFalse(hasIndexPack(name2));
	}

	@Test
	public void testBasicPackagesByHand() throws JevernoteException, IOException {
		final String name1 = "foo";
		final String name2 = "bar";

		createPackageByHand(name1);
		assertTrue(dirOfPack(name1).isDirectory());
		assertFalse(dirOfPack(name2).isDirectory());
		assertFalse(hasIndexPack(name1));
		assertFalse(hasIndexPack(name2));

		movePackageByHand(name1, name2);
		assertFalse(dirOfPack(name1).isDirectory());
		assertTrue(dirOfPack(name2).isDirectory());
		assertFalse(hasIndexPack(name1));
		assertFalse(hasIndexPack(name2));

		removePackageByHand(name2);
		assertFalse(dirOfPack(name2).isDirectory());
		assertFalse(dirOfPack(name2).isDirectory());
		assertFalse(hasIndexPack(name1));
		assertFalse(hasIndexPack(name2));
	}

	@Test
	public void testBasicItems() throws JevernoteException, IOException {
		final String packName1 = "foo";
		final String packName2 = "bar";
		final String name1 = "Lorem";
		final String name2 = "Ipsum";

		createPackage(WITH_ID, packName1);
		createPackage(WITH_ID, packName2);

		createItem(WITH_ID, packName1, name1, "Something #1");
		assertTrue(fileOfItem(packName1, name1).isFile());
		assertFalse(fileOfItem(packName2, name2).isFile());
		assertTrue(hasIndexItem(packName1, name1));
		assertFalse(hasIndexItem(packName2, name2));

		moveItem(WITH_ID, packName1, name1, packName2, name2);
		assertFalse(fileOfItem(packName1, name1).isFile());
		assertTrue(fileOfItem(packName2, name2).isFile());
		assertFalse(hasIndexItem(packName1, name1));
		assertTrue(hasIndexItem(packName2, name2));

		removeItem(WITH_ID, packName2, name2);
		assertFalse(fileOfItem(packName1, name1).isFile());
		assertFalse(fileOfItem(packName2, name2).isFile());
		assertFalse(hasIndexItem(packName1, name1));
		assertFalse(hasIndexItem(packName2, name2));
	}

	@Test
	public void testBasicItemsByHand() throws JevernoteException, IOException {
		final String packName1 = "foo";
		final String packName2 = "bar";
		final String name1 = "Lorem";
		final String name2 = "Ipsum";

		createPackage(WITH_ID, packName1);
		createPackage(WITH_ID, packName2);

		createItemByHand(packName1, name1, "Something #1");
		assertTrue(fileOfItem(packName1, name1).isFile());
		assertFalse(fileOfItem(packName2, name2).isFile());
		assertFalse(hasIndexItem(packName1, name1));
		assertFalse(hasIndexItem(packName2, name2));

		moveItemByHand(packName1, name1, packName2, name2);
		assertFalse(fileOfItem(packName1, name1).isFile());
		assertTrue(fileOfItem(packName2, name2).isFile());
		assertFalse(hasIndexItem(packName1, name1));
		assertFalse(hasIndexItem(packName2, name2));

		removeItemByHand(packName2, name2);
		assertFalse(fileOfItem(packName1, name1).isFile());
		assertFalse(fileOfItem(packName2, name2).isFile());
		assertFalse(hasIndexItem(packName1, name1));
		assertFalse(hasIndexItem(packName2, name2));
	}

	@Test
	public void testCutTghrought() throws JevernoteException, IOException {
		final String packName1 = "foo";
		final String packName2 = "bar";
		final String name1 = "Lorem";
		final String name2 = "Ipsum";

		createPackage(WITH_ID, packName1);

		createItem(WITH_ID, packName1, name1, "Something #1");

		createPackage(WITH_ID, packName2);

		moveItem(WITH_ID, packName1, name1, packName2, name2);

		updateItem(WITH_ID, packName2, name2, "Something absoluttely else #2");

		removePackage(WITH_ID, packName1);

		removeItem(WITH_ID, packName2, name2);

		createItem(WITH_ID, packName2, name1, "Just nothing ... #3");

		updateItem(WITH_ID, packName2, name1, "Really nothing ... #4");

		removeItem(WITH_ID, packName2, name1);
	}

	///////////////////////////////////////////////////////////////////////////

	protected static void createPackage(boolean withId, String name) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Package pack = TestingUtils.createPackageObj(withId, name);
		storage.createPackage(pack);

		finishStorage(storage);
	}

	protected static void createPackageByHand(String name) throws IOException {
		File dir = dirOfPack(name);

		Files.createDirectory(dir.toPath());
	}

	protected static void createItem(boolean withId, String packName, String name, String content)
			throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Item item = TestingUtils.createItemObj(withId, packName, name, content);
		storage.createItem(item);

		finishStorage(storage);
	}

	protected static void createItemByHand(String packName, String name, String content) throws IOException {
		File file = fileOfItem(packName, name);

		byte[] bytes = content.getBytes();
		Files.write(file.toPath(), bytes);
	}

	protected static void movePackage(boolean withId, String oldName, String newName) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Package oldPack = TestingUtils.createPackageObj(withId, oldName);
		Package newPack = oldPack.copy();

		newPack.setName(newName);
		storage.movePackage(oldPack, newPack);

		finishStorage(storage);
	}

	protected static void movePackageByHand(String oldName, String newName) throws IOException {
		File oldDir = dirOfPack(oldName);
		File newDir = dirOfPack(newName);

		Files.move(oldDir.toPath(), newDir.toPath());
	}

	protected static void moveItem(boolean withId, String oldPackName, String oldName, String newPackName,
			String newName) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Item oldItem = TestingUtils.createItemObj(withId, oldPackName, oldName, null);
		Item newItem = oldItem.copy();

		newItem.getPack().setName(newPackName);
		newItem.setName(newName);
		storage.moveItem(oldItem, newItem);

		finishStorage(storage);
	}

	protected static void moveItemByHand(String oldPackName, String oldName, String newPackName, String newName)
			throws IOException {
		File oldFile = fileOfItem(oldPackName, oldName);
		File newFile = fileOfItem(newPackName, newName);

		Files.move(oldFile.toPath(), newFile.toPath());
	}

	protected static void updateItem(boolean withId, String packName, String name, String newContent)
			throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Item item = TestingUtils.createItemObj(withId, packName, name, newContent);
		storage.updateItem(item);

		finishStorage(storage);
	}

	protected static void updateItemByHand(String packName, String name, String newContent) throws IOException {
		File file = fileOfItem(packName, name);

		byte[] bytes = newContent.getBytes();
		Files.write(file.toPath(), bytes);
	}

	protected static void removePackage(boolean withId, String name) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Package pack = TestingUtils.createPackageObj(withId, name);
		storage.removePackage(pack);

		finishStorage(storage);
	}

	protected static void removePackageByHand(String name) throws IOException {
		File dir = dirOfPack(name);

		Files.delete(dir.toPath());
	}

	protected static void removeItem(boolean withId, String packName, String name) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		Item item = TestingUtils.createItemObj(withId, packName, name, null);
		storage.removeItem(item);

		finishStorage(storage);
	}

	protected static void removeItemByHand(String packName, String name) throws IOException {
		File file = fileOfItem(packName, name);

		Files.delete(file.toPath());
	}

	///////////////////////////////////////////////////////////////////////////

	private static XXXFSwIndexFileStorageWrapper initStorage() throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = new XXXFSwIndexFileStorageWrapper(baseDir);

		storage.initialize();

		return storage;
	}

	private static void finishStorage(XXXFSwIndexFileStorageWrapper storage) throws JevernoteException {
		storage.finish();
	}
	///////////////////////////////////////////////////////////////////////////

	protected static boolean hasIndexPack(String name) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		File dir = dirOfPack(name);
		return storage.getBindings().values().contains(dir);
	}

	protected static boolean hasIndexItem(String packName, String name) throws JevernoteException {
		XXXFSwIndexFileStorageWrapper storage = initStorage();

		File file = fileOfItem(packName, name);
		return storage.getBindings().values().contains(file);
	}
	///////////////////////////////////////////////////////////////////////////

	@BeforeClass
	public static void setUpBeforeClass() {
		final String dirName = "jevernote" + "-" + FileSystemStorageWithIndexFileTest.class.getSimpleName() + "-";

		try {
			Log.write("Creating working tmp directory...");
			Path path = Files.createTempDirectory(dirName);
			baseDir = path.toFile();
		} catch (Exception e) {
			assumeNoException(e);
			Log.warn("Cannot create temp directory, skipping test "
					+ FileSystemStorageWithIndexFileTest.class.getName());
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws IOException, JevernoteException {
		delete(baseDir);
		Log.write("Working tmp directory deleted!");

	}

	@Before
	public void setUp() throws Exception {
		XXXFSwIndexFileStorageWrapper storage = new XXXFSwIndexFileStorageWrapper(baseDir);
		try {
			storage.install();
		} catch (JevernoteException e) {
			assumeNoException(e);
			Log.warn("Cannot install, skipping test " + FileSystemStorageWithIndexFileTest.class.getName());
		}
	}

	@After
	public void tearDown() throws Exception {
		XXXFSwIndexFileStorageWrapper storage = new XXXFSwIndexFileStorageWrapper(baseDir);
		storage.uninstall();

		Arrays//
				.stream(baseDir.listFiles()) //
				.forEach((f) -> { //
					try {
						delete(f);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
	}

	///////////////////////////////////////////////////////////////////////////

	private static File dirOfPack(String name) {
		return new File(baseDir, name);
	}

	private static File fileOfItem(String packName, String name) {
		return new File(new File(baseDir, packName), name);
	}

	private static void delete(File file) throws IOException {
		if (file.isDirectory()) {
			for (File sub : file.listFiles()) {
				delete(sub);
			}
		}

		Files.delete(file.toPath());
	}

}
