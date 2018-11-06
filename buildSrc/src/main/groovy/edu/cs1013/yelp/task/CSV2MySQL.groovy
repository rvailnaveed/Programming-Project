package edu.cs1013.yelp.task

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset
import java.nio.file.Paths
import java.sql.Connection
import java.sql.PreparedStatement

class CSV2MySQL extends DefaultTask {
	def static final ID_LENGTH = 22
	def static final ID_TYPE = "char(${ID_LENGTH}) NOT NULL"

	def mode
	def csvPath = project.csvPath
	def dbSettings = new Properties()

	Connection dbConn
	def existingCategories = []

	def createParser(filename) {
		return CSVParser.parse(Paths.get(csvPath, filename), Charset.defaultCharset(),
				CSVFormat.RFC4180.withFirstRecordAsHeader())
	}

	def initDb() {
		getClass().getResource('/database.properties').withInputStream {
			stream -> dbSettings.load(stream)
		}

		if (dbSettings.type != 'mysql') {
			throw new IllegalArgumentException("This script only supports MySQL databases (you tried '${dbSettings.type}')")
		}
		MysqlDataSource dataSource = new MysqlDataSource()
		dataSource.setServerName(dbSettings.server)
		dataSource.setPort(dbSettings.port as int)
		dataSource.setDatabaseName(dbSettings.database)
		dataSource.useServerPreparedStmts = false
		dataSource.rewriteBatchedStatements = true
		dbConn = dataSource.getConnection(dbSettings.username, dbSettings.password)
	}
	def createTable(name, Map<String, String> columns) {
		def columnString = ''
		columns.eachWithIndex {
			colName, type, i ->
				columnString += "${colName} ${type}"
				if (i != columns.size() - 1) columnString += ", "
		}
		return dbConn.createStatement().execute("CREATE TABLE ${name} (${columnString})")
	}
	def dropTable(name) {
		dbConn.createStatement().execute('SET FOREIGN_KEY_CHECKS = 0')
		dbConn.createStatement().execute("DROP TABLE IF EXISTS ${name}")
		dbConn.createStatement().execute('SET FOREIGN_KEY_CHECKS = 1')
	}
	def static getPlaceholders(int n) {
		def placeholders = ""
		for (i in 1..n) {
			placeholders += "?"
			if (i != n) placeholders += ", "
		}

		return placeholders
	}
	def getInsertStatement(table, int n) {
		return dbConn.prepareStatement("INSERT INTO ${table} VALUES (${getPlaceholders(n)})")
	}
	def getInsertStatement(table, List<String> columns) {
		return dbConn.prepareStatement("INSERT INTO ${table} (${columns.join(", ")}) VALUES (${getPlaceholders(columns.size())})")
	}
	def getUpdateStatement(table, idField, column) {
		return dbConn.prepareStatement("UPDATE ${table} SET ${column} = ? WHERE ${idField} = ?")
	}

	def importUsers() {
		dropTable('users')
		createTable('users', [
		        'id': ID_TYPE,
				'name': 'tinytext NOT NULL',
				'review_count': 'int NOT NULL',
				'creation_date': 'date NOT NULL',
				'friends': 'mediumtext',
				'useful': 'int NOT NULL',
				'funny': 'int NOT NULL',
				'cool': 'int NOT NULL',
				'fans': 'int NOT NULL',
				'average_stars': 'fixed(3,2) NOT NULL',

				'PRIMARY KEY': '(id)',
				'INDEX (name(255))': '',
				'INDEX (review_count)': '',
				'INDEX (creation_date)': '',
				'INDEX (useful)': '',
				'INDEX (funny)': '',
				'INDEX (cool)': '',
				'INDEX (fans)': '',
				'INDEX (average_stars)': ''
		])

		def parser = createParser('yelp_user.csv')
		def s = getInsertStatement('users', 10)
		parser.eachWithIndex {
			record, i ->
				def friends = record.get('friends')

				s.setString(1, record.get('user_id'))
				s.setString(2, record.get('name'))
				s.setInt(3, record.get('review_count') as int)
				s.setString(4, record.get('yelping_since'))
				s.setString(5, friends == 'None' ? null : friends.split(', ').join(','))
				s.setInt(6, record.get('useful') as int)
				s.setInt(7, record.get('funny') as int)
				s.setInt(8, record.get('cool') as int)
				s.setInt(9, record.get('fans') as int)
				s.setString(10, record.get('average_stars'))
				s.addBatch()

				if (i % 100 == 0) {
					s.executeBatch()
				}
		}
		s.executeBatch()
		s.close()
	}
	def insertCategory(PreparedStatement cStatement, PreparedStatement cbStatement, businessId, category) {
		if (!(category in existingCategories)) {
			cStatement.setString(1, null)
			cStatement.setString(2, category)
			cStatement.addBatch()

			existingCategories.add(category)
		}

		cbStatement.setString(1, null)
		cbStatement.setString(2, businessId)
		cbStatement.setInt(3, existingCategories.indexOf(category) + 1)
		cbStatement.addBatch()
	}
	def importBusinesses() {
		dropTable('businesses')
		dropTable('categories')
		dropTable('business_category')

		createTable('businesses', [
				'id': ID_TYPE,
				'name': 'tinytext NOT NULL',
				'neighbourhood': 'tinytext',
				'address': 'text',
				'city': 'tinytext',
				'state': 'varchar(128)',
				'postal_code': 'varchar(64)',
				'latitude': 'fixed(13,10)',
				'longitude': 'fixed(13,10)',
				'stars': 'fixed(2,1) NOT NULL',
				'review_count': 'int NOT NULL',
				'is_open': 'boolean NOT NULL',

				'PRIMARY KEY': '(id)',
				'INDEX (name(255))': '',
				'INDEX (neighbourhood(255))': '',
				'INDEX (address(255))': '',
				'INDEX (city(255))': '',
				'INDEX (state)': '',
				'INDEX (postal_code)': '',
				'INDEX (review_count)': '',
				'INDEX (is_open)': ''

		])
		createTable('categories', [
				'id': 'int NOT NULL AUTO_INCREMENT',
				'name': 'tinytext NOT NULL',

				'PRIMARY KEY': '(id)',
				'UNIQUE': '(name(128))'
		])
		createTable('business_category', [
				'id': 'int NOT NULL AUTO_INCREMENT',
				'business_id': ID_TYPE,
				'category_id': 'int NOT NULL',

				'PRIMARY KEY': '(id)',
				'FOREIGN KEY (business_id)': 'REFERENCES businesses(id)',
				'FOREIGN KEY (category_id)': 'REFERENCES categories(id)'
		])

		def parser = createParser('yelp_business.csv')
		def s = getInsertStatement('businesses', 12)
		def cStatement = getInsertStatement('categories', 2)
		def cbStatement = getInsertStatement('business_category', 3)
		parser.eachWithIndex {
			record, i ->
				def id = record.get('business_id')
				def name = record.get('name')
				def neighbourhood = record.get('neighborhood')
				def address = record.get('address')
				if (address.size() > 0) address = address.substring(1, address.size() - 1)
				def postalCode = record.get('postal_code')
				def latitude = record.get('latitude')
				def longitude = record.get('longitude')

				s.setString(1, id)
				s.setString(2, name.substring(1, name.size() - 1))
				s.setString(3, neighbourhood.size() > 0 ? neighbourhood : null)
				s.setString(4, address.size() > 0 ? address : null)
				s.setString(5, record.get('city'))
				s.setString(6, record.get('state'))
				s.setString(7, postalCode.size() > 0 ? postalCode : null)
				s.setString(8, latitude.size() > 0 ? latitude : null)
				s.setString(9, longitude.size() > 0 ? longitude : null)
				s.setString(10, record.get('stars'))
				s.setInt(11, record.get('review_count') as int)
				s.setBoolean(12, record.get('is_open') as boolean)
				s.addBatch()

				record.get('categories').split(';').each {
					category -> insertCategory(cStatement, cbStatement, id, category)
				}
				if (i % 100 == 0) {
					s.executeBatch()
					cStatement.executeBatch()
					cbStatement.executeBatch()
				}
		}
		s.executeBatch()
		s.close()
	}
	def importReviews() {
		dropTable('reviews')
		createTable('reviews', [
				'id': ID_TYPE,
				'user_id': ID_TYPE,
				'business_id': ID_TYPE,
				'stars': 'tinyint NOT NULL',
				'date': 'date NOT NULL',
				'useful': 'int NOT NULL',
				'funny': 'int NOT NULL',
				'cool': 'int NOT NULL',
				'text': 'text NOT NULL',

				'PRIMARY KEY': '(id)',
				'FOREIGN KEY (user_id)': 'REFERENCES users(id)',
				'FOREIGN KEY (business_id)': 'REFERENCES businesses(id)',
				'INDEX (stars)': '',
				'INDEX (date)': '',
				'INDEX (useful)': '',
				'INDEX (funny)': '',
				'INDEX (cool)': ''
		])

		def parser = createParser('yelp_review.csv')
		def s = getInsertStatement('reviews', 9)
		parser.eachWithIndex {
			record, i ->
				s.setString(1, record.get('review_id'))
				s.setString(2, record.get('user_id'))
				s.setString(3, record.get('business_id'))
				s.setInt(4, record.get('stars') as int)
				s.setString(5, record.get('date'))
				s.setInt(6, record.get('useful') as int)
				s.setInt(7, record.get('funny') as int)
				s.setInt(8, record.get('cool') as int)
				s.setString(9, record.get('text'))
				s.addBatch()

				if (i % 100 == 0) s.executeBatch()
		}
		s.executeBatch()
		s.close()
	}
	def importTips() {
		dropTable('tips')
		createTable('tips', [
				'id': 'int NOT NULL AUTO_INCREMENT',
				'user_id': ID_TYPE,
				'business_id': ID_TYPE,
				'likes': 'int NOT NULL',
				'date': 'date NOT NULL',
				'text': 'text NOT NULL',

				'PRIMARY KEY': '(id)',
				'FOREIGN KEY (user_id)': 'REFERENCES users(id)',
				'FOREIGN KEY (business_id)': 'REFERENCES businesses(id)',
				'INDEX (likes)': '',
				'INDEX (date)': '',
		])

		def parser = createParser('yelp_tip.csv')
		def s = getInsertStatement('tips', 6)
		parser.eachWithIndex {
			record, i ->
				s.setString(1, null)
				s.setString(2, record.get('user_id'))
				s.setString(3, record.get('business_id'))
				s.setInt(4, record.get('likes') as int)
				s.setString(5, record.get('date'))
				s.setString(6, record.get('text'))
				s.addBatch()

				if (i % 100 == 0) s.executeBatch()
		}
		s.executeBatch()
		s.close()
	}
	@TaskAction
	def importData() {
		initDb()

		switch (mode) {
		case 'users':
			importUsers()
			break
		case 'businesses':
			importBusinesses()
			break
		case 'reviews':
			importReviews()
			break
		case 'tips':
			importTips()
			break
		}

		dbConn.close()
	}
}
