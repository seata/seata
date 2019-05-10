/*
 /*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import java.util.HashSet;

/**
 * RpcContext Test
 * 
 * @author kaitithoma
 * @author Danaykap
 * 
 * @date 2019/ 3/31
 *
 */

public class RpcContextTest {

	private static RpcContext rc;
	private static final String version = "a"; // Version
	private static final String id = "1"; // ID
	private static final String tsg = "a"; // TransactionServiceGroup
	private static final String rv = "abc"; // ResourceValue
	private static final String rs = "b"; // ResourceSet

	/**
	 * RpcContext Constructor
	 */

	@BeforeAll
	public static void setup() {
		rc = new RpcContext();
	}

	/**
	 * Test set ApplicationId to value = "1" Test get ApplicationId
	 */
	@Test
	public void testApplicationIdValue() {
		rc.setApplicationId(id);
		Assertions.assertEquals(id, rc.getApplicationId());
	}

	/**
	 * Test set Version to value = "a" Test get Version
	 */
	@Test
	public void testVersionValue() {
		rc.setVersion(version);
		Assertions.assertEquals(version, rc.getVersion());
	}

	/**
	 * Test set ClientId to value = "1" Test get ClientId
	 */
	@Test
	public void testClientIdValue() {
		rc.setClientId(id);
		Assertions.assertEquals(id, rc.getClientId());
	}

	/**
	 * Test set Channel to null Test get Channel
	 */
	@Test
	public void testChannelNull() {
		rc.setChannel(null);
		Assertions.assertNull(rc.getChannel());
	}

	/**
	 * Test set TransactionServiceGroup to value = "1" Test get
	 * TransactionServiceGroup
	 */
	@Test
	public void testTransactionServiceGroupValue() {
		rc.setTransactionServiceGroup(tsg);
		Assertions.assertEquals(tsg, rc.getTransactionServiceGroup());
	}

	/**
	 * Test setClientRole to null Test getApplication Id
	 */
	@Test
	public void testClientRoleNull() {
		rc.setClientRole(null);
		Assertions.assertNull(rc.getClientRole());
	}

	/**
	 * Test set ResourceSets to null Test get ResourceSets
	 */
	@Test
	public void testResourceSetsNull() {
		rc.setResourceSets(null);
		Assertions.assertNull(rc.getResourceSets());
	}

	/**
	 * Test add resourceSet = null with addResource Test get ResourceSets
	 */
	@Test
	public void testAddResourceNull() {
		rc.addResource(null);
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(null);
		Assertions.assertEquals(resourceSet, rc.getResourceSets());
	}

	/**
	 * Test add null parameter to ResourceSets with addResources Test get
	 * ResourceSets
	 */
	@Test
	public void testAddResourcesNull() {
		rc.addResources(null);
		rc.setResourceSets(null);
		Assertions.assertNull(rc.getResourceSets());
	}

	/**
	 * Test add a short resourceSet(["abc"]) with addResources Test get ResourceSets
	 */
	@Test
	public void testAddResourcesResourceValue() {
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(rv);
		rc.addResources(resourceSet);
		Assertions.assertEquals(resourceSet, rc.getResourceSets());
	}

	/**
	 * Test add resource and resource sets to ResourceSets with addResourceSets Test
	 * getResourceSets
	 */
	@Test
	public void testAddResourcesResourceSetValue() {
		HashSet<String> resourceSets = new HashSet<String>();
		resourceSets.add(rs);
		HashSet<String> resourceSet = new HashSet<String>();
		resourceSet.add(rv);
		rc.addResources(resourceSet);
		rc.setResourceSets(resourceSets);
		rc.addResources(resourceSet);
		Assertions.assertEquals(resourceSets, rc.getResourceSets());
	}

	/**
	 * Test toString having all the parameters initialized to null
	 */
	@Test
	public void testToString() {
		rc.setApplicationId(null);
		rc.setTransactionServiceGroup(null);
		rc.setClientId(null);
		rc.setChannel(null);
		rc.setResourceSets(null);
		Assertions.assertEquals(
				"RpcContext{" + "applicationId='" + rc.getApplicationId() + '\'' + ", transactionServiceGroup='"
						+ rc.getTransactionServiceGroup() + '\'' + ", clientId='" + rc.getClientId() + '\''
						+ ", channel=" + rc.getChannel() + ", resourceSets=" + rc.getResourceSets() + '}',
				rc.toString());
	}

}