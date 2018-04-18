package com.rreganjr.requel.user.impl;

import com.rreganjr.TestCase;
import com.rreganjr.requel.user.*;
import com.rreganjr.requel.user.exception.NoSuchRoleForUserException;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author rreganjr
 */
public class UserImplTest extends TestCase {

    private final String defaultName = "My Name";
    private final String defaultUsername = "username";
    private final String defaultPassword = "p4ssw0rd";
    private final String defaultEmailAddress = "my@email.com";
    private final String defaultPhoneNumber = "1234567890";
    private final Organization defaultOrganization = new OrganizationImpl("Organization");
    private final boolean defaultEditable = true;
    private final Set<UserRole> defaultUserRoles = new HashSet<>();

    public void testPrimaryConstructor() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        TestCase.assertEquals(defaultUsername, user.getUsername());
        assertTrue(user.isPassword(defaultPassword));
        assertEquals(defaultName, user.getName());
        assertEquals(defaultEmailAddress, user.getEmailAddress());
        assertEquals(defaultPhoneNumber, user.getPhoneNumber());
        assertEquals(defaultOrganization, user.getOrganization());
        assertEquals(defaultEditable, user.isEditable());
        TestCase.assertEquals(defaultUserRoles, user.getUserRoles()); // by default the roles are empty.
        assertNotNull(user.getDescriptiveName());
    }

    public void testNullName() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, null, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        assertEquals(defaultUsername, user.getUsername());
        assertTrue(user.isPassword(defaultPassword));
        assertNull(user.getName());
        assertEquals(defaultEmailAddress, user.getEmailAddress());
        assertEquals(defaultPhoneNumber, user.getPhoneNumber());
        assertEquals(defaultOrganization, user.getOrganization());
        assertEquals(defaultEditable, user.isEditable());
        TestCase.assertEquals(defaultUserRoles, user.getUserRoles()); // by default the roles are empty.
        assertNotNull(user.getDescriptiveName()); // if name is null a descriptive name is still generated
    }

    public void testNoArgsConstructor() {
        UserImpl user = new UserImpl();
        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getUserRoles());
        assertNull(user.getName());
        assertNull(user.getEmailAddress());
        assertNull(user.getPhoneNumber());
        assertNull(user.getOrganization());
        assertFalse(user.isPassword(defaultPassword));
    }

    public void testResetPassword() {
        final String newPassword = "newPassword";
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.resetPassword(newPassword);
        assertTrue("the new password is saved properly.", user.isPassword(newPassword));
    }

    public void testResetPasswordWithSpaces() {
        final String newPassword = " new Password ";
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.resetPassword(newPassword);
        assertTrue("passwords can contain spaces at the ends and between non-space characters.", user.isPassword(newPassword));
    }

    public void testResetPasswordWithLongPassword() {
        final String newPassword = new String(new char[UserImpl.MAX_PASSWORD_LENGTH]).replace("\0", "A");
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.resetPassword(newPassword);
        assertTrue("a password of up to " + UserImpl.MAX_PASSWORD_LENGTH + " characters is supported.", user.isPassword(newPassword));
    }

    public void testResetPasswordUpdateAlgorithm() throws Exception {
        final String newPassword = "MyN3wP4ssw0rd";
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);

        // By design setPasswordEncryptingAlgorithmName is private, the user class manages all this internally.
        // I don't want these parts to be accessible or changeable.
        // I want to make sure the resetPassword/isPassword properly upgrades the encryption method when an
        // updated UserImpl class is used with existing/old code.
        String preferredPasswordEncryptingAlgorithm = TestCase.getPrivateFieldValue(user, "PREFERRED_PASSWORD_ENCRYPTING_ALGORITHM", String.class);
        Integer preferredPasswordEncryptingIterations = TestCase.getPrivateFieldValue(user, "PREFERRED_PASSWORD_ENCRYPTING_ITERATIONS", Integer.class);
        TestCase.callPrivateMethod(user, "setPasswordEncryptingAlgorithmName", "MD5");
        TestCase.callPrivateMethod(user, "setPasswordEncryptingIterations", 1);
        String passwordSalt = (String) TestCase.callPrivateMethod(user, "getPasswordSalt");

        user.resetPassword(newPassword);

        assertEquals("reset password updates encryption algorithm on password reset", preferredPasswordEncryptingAlgorithm, TestCase.callPrivateMethod(user, "getPasswordEncryptingAlgorithmName"));
        assertEquals("reset password updates encryption iterations on password reset", preferredPasswordEncryptingIterations, TestCase.callPrivateMethod(user, "getPasswordEncryptingIterations"));
        assertNotSame("reset the password salt on password reset", passwordSalt, TestCase.callPrivateMethod(user, "getPasswordEncryptingIterations"));
        assertTrue("the new password is saved properly.", user.isPassword(newPassword));
    }

    public void testIsPasswordUpdateAlgorithm() throws Exception {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);

        // By design setPasswordEncryptingAlgorithmName is private, the user class manages all this internally.
        // I don't want these parts to be accessible or changeable.
        // I want to make sure the resetPassword/isPassword properly upgrades the encryption method when an
        // updated UserImpl class is used with existing/old code.
        String preferredPasswordEncryptingAlgorithm = TestCase.getPrivateFieldValue(user, "PREFERRED_PASSWORD_ENCRYPTING_ALGORITHM", String.class);
        Integer preferredPasswordEncryptingIterations = TestCase.getPrivateFieldValue(user, "PREFERRED_PASSWORD_ENCRYPTING_ITERATIONS", Integer.class);
        TestCase.callPrivateMethod(user, "setPasswordEncryptingAlgorithmName", "MD5");
        TestCase.callPrivateMethod(user, "setPasswordEncryptingIterations", 1);
        String passwordSalt = (String) TestCase.callPrivateMethod(user, "getPasswordSalt");
        // we need to reset the password so that it is encoded with the lower algorithm and iterations so it matches in isPassword
        TestCase.callPrivateMethod(user, "setEncryptedPassword", TestCase.callPrivateMethod(user, "encryptPassword", defaultPassword));

        user.isPassword(defaultPassword);

        assertEquals("reset password updates encryption algorithm on login if outdated.", preferredPasswordEncryptingAlgorithm, TestCase.callPrivateMethod(user, "getPasswordEncryptingAlgorithmName"));
        assertEquals("reset password updates encryption iterations on login if outdated.", preferredPasswordEncryptingIterations, TestCase.callPrivateMethod(user, "getPasswordEncryptingIterations"));
        assertNotSame("reset the password salt on login if algorithm or iterations is outdated.", passwordSalt, TestCase.callPrivateMethod(user, "getPasswordEncryptingIterations"));
        assertTrue("original password still works after logging in and updating encryption algorithm, iterations and password salt.", user.isPassword(defaultPassword));
    }

    public void testInvalidPasswordEncryptingAlgorithm() throws Exception {
        final String badAlgorithmName = "NoSuchAlgorithm";
        // I'm not sure this is possible, but I want to make sure the exception is thrown properly if in that state.
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        TestCase.callPrivateMethod(user, "setPasswordEncryptingAlgorithmName", badAlgorithmName);
        try {
            TestCase.callPrivateMethod(user, "encryptPassword", "NewPassword");
            fail("Expected PasswordException exception");
        } catch (InvocationTargetException | PasswordException e) {
            assertEquals(PasswordException.badAlgorithmName(badAlgorithmName), ((e instanceof InvocationTargetException)?((InvocationTargetException) e).getCause():e));
        }

    }

    public void testResetPasswordTooLong() {
        // resetPassword doesn't throw an exception because of the way validation works so that
        // errors for all the properties are returned together.
        final String newPassword = new String(new char[UserImpl.MAX_PASSWORD_LENGTH]).replace("\0", "A");
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        // NOTE: this doesn't throw an exception so that all fields can be validated on change together.
        user.resetPassword(newPassword);
        assertFalse("resetting the password with an invalid value doesn't take.", user.isPassword(newPassword));
        assertTrue("resetting the password with an invalid value the original password still works.", user.isPassword(defaultPassword));
    }

    public void testResetPasswordTooShort() {
        final String newPassword = "";
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.resetPassword(newPassword);
        assertFalse("resetting the password with an invalid value doesn't take.", user.isPassword(newPassword));
        assertTrue("resetting the password with an invalid value the original password still works.", user.isPassword(defaultPassword));
    }

    public void testResetPasswordOnlyWhitespace() {
        final String newPassword = "      ";
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.resetPassword(newPassword);
        assertFalse("resetting the password with an invalid value doesn't take.", user.isPassword(newPassword));
        assertTrue("resetting the password with an invalid value the original password still works.", user.isPassword(defaultPassword));
    }

    public void testResetPasswordInvalidEncryptedNotNull() throws Exception {
        final String newPassword = "";
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        TestCase.callPrivateMethod(user, "setEncryptedPassword", new ParamTypeAndValue(String.class, null));
        user.resetPassword(newPassword);
        assertNotNull(TestCase.callPrivateMethod(user, "getEncryptedPassword"));
        assertEquals("null password gets set to \"\" if setting a non-valid password", "", TestCase.callPrivateMethod(user, "getEncryptedPassword"));
        assertFalse("trying to reset the password when no password existed before sets the hashedPassword to empty string so validation of all fields will complete.", user.isPassword(newPassword));
    }


    public void testUserRoles() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        assertFalse(user.hasRole(SystemAdminUserRole.class));
        assertFalse(user.hasRole(DomainAdminUserRole.class));
        try {
            user.getRoleForType(SystemAdminUserRole.class);
            fail("expected NoSuchRoleForUserException but no exception was thrown.");
        } catch (NoSuchRoleForUserException e) {
            assertEquals(NoSuchRoleForUserException.forUserRoleTypeName(user, SystemAdminUserRole.class), e);
        }
        user.grantRole(SystemAdminUserRole.class);
        assertNotNull(user.getRoleForType(SystemAdminUserRole.class));

        user.grantRole(DomainAdminUserRole.class);
        assertNotNull(user.getRoleForType(DomainAdminUserRole.class));

        assertEquals(2, user.getUserRoles().size());
        assertTrue(user.hasRole(SystemAdminUserRole.class));
        assertTrue(user.hasRole(DomainAdminUserRole.class));

        user.revokeRole(DomainAdminUserRole.class);
        try {
            user.getRoleForType(DomainAdminUserRole.class);
            fail("expected DomainAdminUserRole but no exception was thrown.");
        } catch (NoSuchRoleForUserException e) {
            assertEquals(NoSuchRoleForUserException.forUserRoleTypeName(user, DomainAdminUserRole.class), e);
        }

        user.revokeRole(SystemAdminUserRole.class);
        try {
            user.getRoleForType(SystemAdminUserRole.class);
            fail("expected SystemAdminUserRole but no exception was thrown.");
        } catch (NoSuchRoleForUserException e) {
            assertEquals(NoSuchRoleForUserException.forUserRoleTypeName(user, SystemAdminUserRole.class), e);
        }

        assertEquals(0, user.getUserRoles().size());
        assertFalse(user.hasRole(SystemAdminUserRole.class));
        assertFalse(user.hasRole(DomainAdminUserRole.class));
    }

    public void testEquals() {
        // User id takes precedence over username
        UserImpl user = new UserImpl();
        assertTrue(user.equals(user));
        assertFalse(user.equals(null));

        UserImpl user2 = new UserImpl();

        assertTrue(user.equals(user2));
        assertTrue(user2.equals(user));

        user.setUsername(defaultUsername);
        assertFalse(user.equals(user2));
        assertFalse(user2.equals(user));

        user2.setUsername(defaultUsername);
        assertTrue(user.equals(user2));
        assertTrue(user2.equals(user));

        assertFalse(user.equals("A string"));

        user.setId(1L);
        user2.setId(1L);
        assertTrue(user.equals(user2));
        assertTrue(user2.equals(user));

        user2.setId(10L);
        assertFalse(user.equals(user2));
        assertFalse(user2.equals(user));
    }

    public void testEqualsById() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        UserImpl user2 = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);

        // by id is only equal when both users have assigned ids that are numerically equal.
        assertFalse(user.equalsById(user2));
        assertFalse(user2.equalsById(user));

        user.setId(1000L);
        assertFalse(user.equalsById(user2));
        assertFalse(user2.equalsById(user));

        user2.setId(10L);
        assertFalse(user.equalsById(user2));
        assertFalse(user2.equalsById(user));

        user2.setId(1000L);
        assertTrue(user.equalsById(user2));
        assertTrue(user2.equalsById(user));
    }

    public void testCompareTo() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        UserImpl user2 = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);

        assertEquals(0, user.compareTo(user2));
        assertEquals(0, user2.compareTo(user));

        user.setUsername("aaaaa");
        user2.setUsername("bbbbb");
        assertTrue( user.compareTo(user2) < 0);
        assertTrue( user2.compareTo(user) > 0);
    }

    public void testHashCode() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        UserImpl user2 = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        assertEquals(user.hashCode(), user2.hashCode());

        // make sure that once the hashcode is calculated it doesn't change
        // this is primarily so HashMap and HashSet operate properly (no duplicates)
        user2.setUsername(defaultUsername + "abcd");
        assertEquals(user.hashCode(), user2.hashCode());

        user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user2 = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.setId(100L);
        user2.setId(100L);
        assertEquals(user.hashCode(), user2.hashCode());

        user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user2 = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user2.setUsername(defaultUsername + "abcd");
        assertNotSame(user.hashCode(), user2.hashCode());
    }

    public void testToString() {
        assertNotNull(new UserImpl().toString());
        assertNotNull(new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable).toString());
    }

    public void testVersion() {
        UserImpl user = new UserImpl(defaultUsername, defaultPassword, defaultName, defaultEmailAddress, defaultPhoneNumber, defaultOrganization, defaultEditable);
        user.setVersion(1000);
        assertEquals(1000, user.getVersion());
    }

    public void testIdAdapter() throws Exception {
        UserImpl.IdAdapter idAdapter = new UserImpl.IdAdapter();
        assertEquals("", idAdapter.marshal(null));
        assertTrue(idAdapter.marshal(1234L).contains("1234"));
        assertNull("unmarshal always returns null for id, the id in the XML file is only relevant in the file", idAdapter.unmarshal("USR_100"));
    }
}
