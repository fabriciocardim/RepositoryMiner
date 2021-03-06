package org.repositoryminer.model;

import java.util.List;

import org.bson.Document;
import org.repositoryminer.scm.SCMType;

public class Repository {

	private String id;
	private String name;
	private String path;
	private String description;
	private SCMType scm;
	private List<Contributor> contributors;

	public Document toDocument() {
		Document doc = new Document();
		doc.append("name", name).append("path", path).append("description", description).append("scm", scm.toString());
		return doc;
	}

	public static Repository parseDocument(Document repositoryDoc) {
		if (repositoryDoc == null)
			return null;
		
		Repository repository = new Repository(repositoryDoc.getObjectId("_id").toString(),
				repositoryDoc.getString("name"), repositoryDoc.getString("path"),
				repositoryDoc.getString("description"), SCMType.valueOf(repositoryDoc.getString("scm")));

		return repository;
	}

	public Repository(org.repositoryminer.mining.RepositoryMiner repo) {
		this(null, repo.getName(), repo.getPath(), repo.getDescription(), repo.getScm());
	}

	public Repository() {
	}
	
	public Repository(String id, String name, String path, String description, SCMType scm) {
		this.id = id;
		this.name = name;
		this.path = path;
		this.description = description;
		this.scm = scm;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SCMType getScm() {
		return scm;
	}

	public void setScm(SCMType scm) {
		this.scm = scm;
	}

	public List<Contributor> getContributors() {
		return contributors;
	}

	public void setContributors(List<Contributor> contributors) {
		this.contributors = contributors;
	}

}