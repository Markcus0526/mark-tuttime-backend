package com.tuttime.admin.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes for admin users.
 *
 * <p>This is a utility class for generating password hashes to be used in
 * database initialization scripts. Run this class's main method to generate
 * a hash for a given password.
 *
 * <p><b>Usage:</b>
 * <pre>
 * java PasswordHashGenerator admin123
 * </pre>
 */
public class PasswordHashGenerator {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: java PasswordHashGenerator <password>");
			System.out.println("Example: java PasswordHashGenerator admin123");
			System.out.println();
			System.out.println("To run with Maven:");
			System.out.println("  mvn test-compile exec:java -Dexec.mainClass=\"com.tuttime.admin.util.PasswordHashGenerator\" -Dexec.args=\"admin123\"");
			System.exit(1);
		}

		String password = args[0];
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = encoder.encode(password);

		System.out.println("========================================");
		System.out.println("BCrypt Password Hash Generator");
		System.out.println("========================================");
		System.out.println("Password: " + password);
		System.out.println("BCrypt Hash: " + hash);
		System.out.println();

		// Verify the hash
		boolean matches = encoder.matches(password, hash);
		System.out.println("Verification: " + (matches ? "✓ Hash matches password" : "✗ Hash does NOT match password"));
		System.out.println();

		System.out.println("SQL UPDATE statement:");
		System.out.println("UPDATE admin_service.admin_users");
		System.out.println("SET password_hash = '" + hash + "'");
		System.out.println("WHERE username = 'admin';");
		System.out.println();

		System.out.println("Or SQL INSERT statement:");
		System.out.println("INSERT INTO admin_users (username, email, password_hash, role, is_active, created_by)");
		System.out.println("VALUES (");
		System.out.println("    'admin',");
		System.out.println("    'admin@tuttime.com',");
		System.out.println("    '" + hash + "', -- " + password);
		System.out.println("    'SUPER_ADMIN',");
		System.out.println("    true,");
		System.out.println("    NULL");
		System.out.println(") ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash;");
	}
}

