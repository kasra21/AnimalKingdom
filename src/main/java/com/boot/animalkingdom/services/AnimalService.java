package com.boot.animalkingdom.services;

import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class AnimalService {

	// Test mysql database connection
	public String mysqlConnectTest() {

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			return "Driver not found";
		}

		Connection connection = null;

		try {
			connection = DriverManager.getConnection("jdbc:mysql://192.168.56.1:3306/test_schema", "admin", "password");

		} catch (SQLException e) {
			return "Connection Failed";
		}

		if (connection != null) {
			return "Connection Successful";
		} else {
			return "Connection Failed";
		}
	}

	private Connection getMySqlConnection() {
		
		String jdbcUrl = "jdbc:mysql://192.168.56.1:3306/test_schema";
		String user = "admin";
		String password = "password";
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			return null;
		}

		Connection connection = null;

		try {
			connection = DriverManager.getConnection(jdbcUrl, user, password);
		} catch (SQLException e) {
			return null;
		}

		return connection;
	}

}
