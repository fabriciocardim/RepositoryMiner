package org.repositoryminer.persistence.handler;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.repositoryminer.persistence.Connection;

import com.mongodb.BasicDBObject;

public class CommitDocumentHandler extends DocumentHandler {

	private static final String COLLECTION_NAME = "rm_commits";

	public CommitDocumentHandler() {
		super.collection = Connection.getInstance().getCollection(COLLECTION_NAME);
	}

	@Override
	public Document findById(String id, Bson projection){
		BasicDBObject whereClause = new BasicDBObject();
		whereClause.put("_id", id);
		
		return findOne(whereClause, projection);
	}
	
	public List<Document> findByIdColl(String repository, List<String> ids, Bson projection) {
		BasicDBObject where = new BasicDBObject(2);
		where.append("repository", new ObjectId(repository));
		where.append("_id", new BasicDBObject("$in", ids));
		return findMany(new BasicDBObject("$and", where), projection);
	}
	
}