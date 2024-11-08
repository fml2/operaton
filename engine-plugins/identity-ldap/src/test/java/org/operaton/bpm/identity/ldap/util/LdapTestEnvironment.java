package org.operaton.bpm.identity.ldap.util;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class LdapTestEnvironment {
  private static final Logger LOG = LoggerFactory.getLogger(LdapTestEnvironment.class);
  private static final String BASE_DN = "o=operaton,c=org";
  private static final String OFFICE_BERKELEY = "office-berkeley";

  protected DirectoryService service;
  private LdapServer ldapService;
  protected File workingDirectory = new File(System.getProperty("java.io.tmpdir") + "/server-work");

  private int numberOfUsersCreated = 0;
  private int numberOfGroupsCreated = 0;
  private int numberOfRolesCreated = 0;

  protected void initSchemaPartition() throws Exception {
    InstanceLayout instanceLayout = service.getInstanceLayout();
    File schemaDir = new File(instanceLayout.getPartitionsDirectory(), "schema");

    if (!schemaDir.exists()) {
      new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory()).extractOrCopy();
    } else {
      LOG.info("schema partition already exists, skipping schema extraction");
    }

    SchemaManager schemaManager = new DefaultSchemaManager(new LdifSchemaLoader(schemaDir));
    schemaManager.loadAllEnabled();

    if (!schemaManager.getErrors().isEmpty()) {
      throw new Exception("Failed to load schema: " + schemaManager.getErrors());
    }

    service.setSchemaManager(schemaManager);
    LdifPartition schemaPartition = new LdifPartition(schemaManager, service.getDnFactory());
    schemaPartition.setPartitionPath(schemaDir.toURI());
    service.setSchemaPartition(new SchemaPartition(schemaManager));
  }

  protected void initializeDirectory() throws Exception {
    workingDirectory.mkdirs();
    service = new DefaultDirectoryService();
    service.setInstanceLayout(new InstanceLayout(workingDirectory));
    initSchemaPartition();

    Partition systemPartition = createPartition("system", ServerDNConstants.SYSTEM_DN);
    service.setSystemPartition(systemPartition);
    service.getChangeLog().setEnabled(false);
    service.setDenormalizeOpAttrsEnabled(true);

    Partition operatonPartition = createPartition("operaton", BASE_DN);
    addIndexes(operatonPartition, "objectClass", "ou", "uid");
    service.startup();

    if (!service.getAdminSession().exists(operatonPartition.getSuffixDn())) {
      createEntry(BASE_DN, "top", "domain", "extensibleObject");
    }
  }

  public void startServer() throws Exception {
    ldapService = new LdapServer();
    ldapService.setTransports(new TcpTransport(Integer.parseInt(loadTestProperties().getProperty("ldap.server.port"))));
    ldapService.setDirectoryService(service);
    ldapService.start();
  }

  public void init() throws Exception {
    /// create a simple test
    init(0,0,0);
  }

  public void init(int users, int groups, int roles) throws Exception {
    initializeDirectory();
    startServer();

    createGroup("office-berlin", "roman", "robert", "daniel", "gonzo", "rowlf", "pepe", "rizzo");
    createGroup("office-london", "oscar", "monster");
    createGroup("office-home", "david(IT)", "ruecker");
    createGroup("office-external", "fozzie");

    createRole("management", "ruecker", "robert", "daniel");
    createRole("development", "roman", "daniel", "oscar");
    createRole("consulting", "ruecker");
    createRole("sales", "ruecker", "monster", "david");
    createRole("external", "fozzie");

    for (int i = 1; i <= users; i++) {
      createUserUid(String.format("jan.fisher.%04d", i), OFFICE_BERKELEY, "jan", "fisher" + String.format("%04d", i), "jan.fisher" + String.format("%04d", i) + "@operaton.org");
    }

    for (int i = 1; i <= groups; i++) {
      createGroup("Paris" + String.format("%04d", i));
    }

    for (int i = 1; i <= roles; i++) {
      createRole("Support" + String.format("%04d", i), "fozzie");
    }
  }

  protected Partition createPartition(String id, String dn) throws Exception {
    JdbmPartition partition = new JdbmPartition(service.getSchemaManager(), service.getDnFactory());
    partition.setId(id);
    partition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(), id).toURI());
    partition.setSuffixDn(new Dn(dn));
    service.addPartition(partition);
    return partition;
  }

  protected void addIndexes(Partition partition, String... attrs) {
    Set<Index<?, String>> indexes = new HashSet<>();
    for (String attr : attrs) {
      indexes.add(new JdbmIndex<>(attr, false));
    }
    ((JdbmPartition) partition).setIndexedAttributes(indexes);
  }

  protected String createUserUid(String uid, String group, String firstname, String lastname, String email) throws Exception {
    Dn dn = new Dn("uid=" + uid + ",ou=" + group + ",o=operaton,c=org");
    if (!service.getAdminSession().exists(dn)) {
      Entry entry = createEntry(dn.getNormName(), "top", "person", "inetOrgPerson");
      entry.add("uid", uid);
      entry.add("cn", firstname);
      entry.add("sn", lastname);
      entry.add("mail", email);
      entry.add("userPassword", uid.getBytes(StandardCharsets.UTF_8));
      service.getAdminSession().add(entry);
      numberOfUsersCreated++;
    }
    return dn.getNormName();
  }

  protected void createGroup(String name, String... members) throws Exception {
    Dn dn = new Dn("ou=" + name + ",o=operaton,c=org");
    if (!service.getAdminSession().exists(dn)) {
      Entry entry = createEntry(dn.getNormName(), "top", "organizationalUnit");
      entry.add("ou", name);
      service.getAdminSession().add(entry);
      numberOfGroupsCreated++;
    }
  }

  protected void createRole(String role, String... members) throws Exception {
    Dn dn = new Dn("ou=" + role + ",o=operaton,c=org");
    if (!service.getAdminSession().exists(dn)) {
      Entry entry = createEntry(dn.getNormName(), "top", "groupOfNames");
      entry.add("cn", role);
      for (String member : members) {
        entry.add("member", member);
      }
      service.getAdminSession().add(entry);
      numberOfRolesCreated++;
    }
  }

  protected Entry createEntry(String dn, String... objectClasses) throws Exception {
    Entry entry = service.newEntry(new Dn(dn));
    entry.add("objectClass", objectClasses);
    return entry;
  }

  public void shutdown() {
    try {
      ldapService.stop();
      service.shutdown();
      if (workingDirectory.exists()) {
        FileUtils.deleteDirectory(workingDirectory);
      }
    } catch (Exception e) {
      LOG.error("exception while shutting down ldap", e);
    }
  }

  protected Properties loadTestProperties() throws IOException {
    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(IoUtil.getFile("ldap.properties"))) {
      props.load(in);
    }
    return props;
  }

  public int getTotalNumberOfUsersCreated() {
    return numberOfUsersCreated;
  }

  public int getTotalNumberOfGroupsCreated() {
    return numberOfGroupsCreated;
  }

  public int getTotalNumberOfRolesCreated() {
    return numberOfRolesCreated;
  }
}
