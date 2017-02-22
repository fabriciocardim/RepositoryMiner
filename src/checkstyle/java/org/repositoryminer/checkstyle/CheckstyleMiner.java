package org.repositoryminer.checkstyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.repositoryminer.checkstyle.model.StyleProblem;
import org.repositoryminer.checkstyle.persistence.CheckstyleDocumentHandler;
import org.repositoryminer.exceptions.RepositoryMinerException;
import org.repositoryminer.model.Commit;
import org.repositoryminer.model.Reference;
import org.repositoryminer.model.Repository;
import org.repositoryminer.persistence.handler.CommitDocumentHandler;
import org.repositoryminer.persistence.handler.ReferenceDocumentHandler;
import org.repositoryminer.persistence.handler.RepositoryDocumentHandler;
import org.repositoryminer.scm.ISCM;
import org.repositoryminer.scm.ReferenceType;
import org.repositoryminer.scm.SCMFactory;
import org.repositoryminer.utility.FileUtils;
import org.repositoryminer.utility.StringUtils;

import com.mongodb.client.model.Projections;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

public class CheckstyleMiner {

	private ISCM scm;
	private String tempRepository;
	
	private Repository repository;

	private CommitDocumentHandler commitPersist = new CommitDocumentHandler();
	private ReferenceDocumentHandler refPersist = new ReferenceDocumentHandler();
	private CheckstyleDocumentHandler checkstylePersist = new CheckstyleDocumentHandler();

	private CheckstyleExecutor checkstyleExecutor;

	private String propertiesFile;
	private String configFile;

	public CheckstyleMiner(Repository repository) {
		this.repository = repository;
	}
	
	public CheckstyleMiner(String repositoryId) {
		final RepositoryDocumentHandler repoHandler = new RepositoryDocumentHandler();
		this.repository = Repository
				.parseDocument(repoHandler.findById(repositoryId, Projections.include("path", "name", "scm")));
	}
	
	public void execute(String hash) {
		persistAnalysis(hash, null);
	}
	
	public void execute(String name, ReferenceType type) {
		final Document refDoc = refPersist.findByNameAndType(name, type, repository.getId(), Projections.slice("commits", 1));
		final Reference reference = Reference.parseDocument(refDoc);

		final String commitId = reference.getCommits().get(0);
		persistAnalysis(commitId, reference);
	}
	
	public void prepare() throws IOException {
		File repositoryFolder = new File(repository.getPath());
		String tempRepository = FileUtils.copyFolderToTmp(repositoryFolder.getAbsolutePath(), repository.getName());
		
		scm = SCMFactory.getSCM(repository.getScm());
		scm.open(tempRepository);
		
		checkstyleExecutor = new CheckstyleExecutor(tempRepository);
	}
	
	public void dispose() throws IOException {
		scm.close();
		FileUtils.deleteFolder(tempRepository);
	}
	
	private void persistAnalysis(String commitId, Reference ref) {
		final Commit commit = Commit.parseDocument(commitPersist.findById(commitId, Projections.include("commit_date")));
		
		scm.checkout(commitId);
		
		configureCheckstyle();
		Map<String, List<StyleProblem>> result = null;
		
		try {
			result = checkstyleExecutor.execute();
		} catch (CheckstyleException e) {
			throw new RepositoryMinerException("Can not execute checkstyle", e);
		}
		
		List<Document> documents = new ArrayList<Document>(result.size());
		for (Entry<String, List<StyleProblem>> file : result.entrySet()) {
			Document doc = new Document();
			
			if (ref != null) {
				doc.append("reference_name", ref.getName());
				doc.append("reference_type", ref.getType().toString());
			}
			
			doc.append("commit", commit.getId());
			doc.append("commit_date", commit.getCommitDate());
			doc.append("repository", new ObjectId(repository.getId()));
			doc.append("filename", file.getKey());
			doc.append("filehash", StringUtils.encodeToCRC32(file.getKey()));
			doc.append("style_problems", StyleProblem.toDocumentList(file.getValue()));
			
			documents.add(doc);
		}
		
		checkstylePersist.insertMany(documents);
	}
	
	private void configureCheckstyle() {
		checkstyleExecutor.setConfigFile(configFile);
		checkstyleExecutor.setPropertiesFile(propertiesFile);
	}
	
	public String getPropertiesFile() {
		return propertiesFile;
	}

	public void setPropertiesFile(String propertiesFile) {
		this.propertiesFile = propertiesFile;
	}

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

}