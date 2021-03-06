package cz.martlin.jevernote.storage.impls;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.thrift.TException;

import cz.martlin.jevernote.dataobj.cmp.Change;
import cz.martlin.jevernote.dataobj.misc.Config;
import cz.martlin.jevernote.dataobj.storage.Item;
import cz.martlin.jevernote.dataobj.storage.Package;
import cz.martlin.jevernote.misc.JevernoteException;
import cz.martlin.jevernote.storage.base.CommonStorage;
import cz.martlin.jevernote.storage.content.base.ContentProcessor;

public class EvernoteStorage extends CommonStorage<Notebook, Note> {

	private final Logger LOG = LoggerFactory.getLogger(getClass());

	private final EvernoteService service;
	private final ContentProcessor proces;
	private final File baseDir;
	
	private NoteStoreClient cli;

	public EvernoteStorage(Config config, File baseDir, EvernoteService service, ContentProcessor proces) {
		super(config);
		
		this.service = service;
		this.proces = proces;
		this.baseDir = baseDir;
	}

	///////////////////////////////////////////////////////////////////////////

	public boolean doIsInstalled() throws Exception {
		return config.existsConfigFile(baseDir);
	}

	@Override
	protected void doInstallAndLoad(String token) throws IOException, JevernoteException {
		config.setAuthToken(token);

		config.save(baseDir);

		cli = createNoteStore(service, token);
	}

	protected void doLoad() throws IOException, JevernoteException {
		config.load(baseDir);

		String token = config.getAuthToken();
		cli = createNoteStore(service, token);
	}

	@Override
	protected void doStore() throws JevernoteException {
		// nothing needed here
	}

	///////////////////////////////////////////////////////////////////////////

	@Override
	protected List<Notebook> listNativePackages() throws EDAMUserException, EDAMSystemException, TException {
		List<Notebook> notebooks = cli.listNotebooks();

		return notebooks;
	}

	@Override
	protected List<Note> listNativeItems(Package pack)
			throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {
		NoteFilter filter = new NoteFilter();
		filter.setNotebookGuid(pack.getId());

		NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
		NotesMetadataList notesMetadataList = cli.findNotesMetadata(filter, 0, Integer.MAX_VALUE, resultSpec);
		List<NoteMetadata> metadatas = notesMetadataList.getNotes();

		List<Note> notes = new ArrayList<>(metadatas.size());
		for (NoteMetadata metadata : metadatas) {
			String guid = metadata.getGuid();
			Note note = cli.getNote(guid, true, false, false, false);
			notes.add(note);
		}

		return notes;
	}

	///////////////////////////////////////////////////////////////////////////

	@Override
	protected void createPackageNative(Package pack, Notebook notebook)
			throws EDAMUserException, EDAMSystemException, TException {
		notebook = cli.createNotebook(notebook);

		String id = notebook.getGuid();
		pack.setId(id);
	}

	@Override
	protected void createItemNative(Item item, Note note)
			throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {
		String pid = item.getPack().getId();
		note.setNotebookGuid(pid);

		note = cli.createNote(note);

		String id = note.getGuid();
		item.setId(id);
	}

	@Override
	protected void movePackageNative(Package oldPack, Package newPack, Notebook oldNotebook, Notebook newNotebook)
			throws Exception {

		cli.updateNotebook(newNotebook);
	}

	@Override
	protected void moveItemNative(Item oldItem, Item newItem, Note oldNote, Note newNote)
			throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {

		cli.updateNote(newNote);
	}

	@Override
	protected void updateItemNative(Item item, Note note)
			throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {

		cli.updateNote(note);
	}

	@Override
	protected void removePackageNative(Package pack, Notebook notebook) {
		LOG.warn("Removing of packages not supported by evernote, package " + pack.getName() + " will not be removed");
	}

	@Override
	protected void removeItemNative(Item item, Note note)
			throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {
		String id = note.getGuid();

		cli.deleteNote(id);

		item.setId(null);
	}

	///////////////////////////////////////////////////////////////////////////

	protected void backupItemNative(Item item, Note nativ) throws Exception {
		LOG.warn("Backup not supported by evernote, item " + item.getName() + " will not be backed up");
	}

	@Override
	protected void backupPackageNative(Package pack, Notebook nativ) throws Exception {
		LOG.warn("Backup not supported by evernote, package " + pack.getName() + " will not be backed up");
	}

	@Override
	public void donePackChangeOnAnother(Change<Package> change) throws JevernoteException {
		// nothing?

	}

	@Override
	public void doneItemChangeOnAnother(Change<Item> change) throws JevernoteException {
		// nothing?
	}

	///////////////////////////////////////////////////////////////////////////

	@Override
	protected Note itemToNative(Item item) {
		Note note = new Note();

		String name = item.getName();
		note.setTitle(name);

		String id = item.getId();
		note.setGuid(id);

		String content = proces.toStorage(item.getContent());
		note.setContent(content);

		return note;
	}

	@Override
	protected Item nativeToItem(Package pack, Note note) {
		String id = note.getGuid();
		String name = note.getTitle();
		String content = proces.fromStorage(note.getContent());

		Calendar lastModifiedAt = toCalendar(note.getUpdated());

		return new Item(pack, id, name, content, lastModifiedAt);

	}

	@Override
	protected Notebook packageToNative(Package pack) {
		Notebook notebook = new Notebook();

		String name = pack.getName();
		notebook.setName(name);

		String id = pack.getId();
		notebook.setGuid(id);

		return notebook;
	}

	@Override
	protected Package nativeToPackage(Notebook notebook) {
		String id = notebook.getGuid();
		String name = notebook.getName();

		return new Package(id, name);
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Connects to the evernote using given auth token.
	 * 
	 * @param token
	 * @return
	 * @throws JevernoteException
	 */
	private NoteStoreClient createNoteStore(EvernoteService service, String token) throws JevernoteException {
		try {
			// https://github.com/evernote/evernote-sdk-java/blob/master/sample/client/EDAMDemo.java
			EvernoteAuth evernoteAuth = new EvernoteAuth(service, token);
			ClientFactory factory = new ClientFactory(evernoteAuth);
			UserStoreClient userStore = factory.createUserStoreClient();

			boolean versionOk = userStore.checkVersion("Evernote EDAMDemo (Java)",
					com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
					com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);

			if (!versionOk) {
				throw new IllegalArgumentException("Incompatible Evernote client protocol version");
			}

			// Set up the NoteStore client
			NoteStoreClient noteStore = factory.createNoteStoreClient();
			return noteStore;
		} catch (Exception e) {
			throw new JevernoteException("Cannot connec to evernote", e);
		}
	}
}
