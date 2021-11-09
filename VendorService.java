

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		ClearCodeAsnPojo ccAsnPojo = ConfigurationBootStrapper.getInstance().getClearCodeObj();
		CounterNameEnum.CNTR_ADD_VENDOR_REQUEST.increment();

		SessionFactory factory = SessionFactory.getSessionFactory();
		Session session = factory.getSession();

		// check vendor using email id if exist return same
		Vendor existingVendor = vendorRepository.getVendorByEmail(session, vendor.getEmail());
		if (existingVendor != null && existingVendor.getId() != null)
			return getVendorInfo(vendor.getEmail());

		try {
			session.beginTransaction();

			List<Roles> roles = new ArrayList<>();

			Roles r=new Roles(); 

			Set<String> setOfRoles=new HashSet<>();
			
			for(Map<String, Object> data: vendor.getRolesAssociatedWithCircle())
			{
				for (Entry<String, Object> key : data.entrySet())

				{
					if (key.getKey().equalsIgnoreCase("roleName")) {
						if (BaseContextMapper.cacheRoles.containsKey(key.getValue().toString().toUpperCase())) {

							r = BaseContextMapper.cacheRoles.get(key.getValue().toString().toUpperCase());

						}
					}

					if (key.getKey().equalsIgnoreCase("circles")) {
						
						List<String>circleCode=(List<String>) key.getValue();
						List<Circle> circleWithRole = new ArrayList<>();
						for (String v : circleCode) {
							
							Circle c = new Circle();
							c.setCode(v);
							circleWithRole.add(c);
							
						}
						
						r.setCircles(circleWithRole);
					}

				}
				
				if(!setOfRoles.contains(r.getRoleName().toUpperCase()))
				{
					roles.add(r);
					setOfRoles.add(r.getRoleName().toUpperCase());
				}
			}

			Vendor v = new Vendor();
			v.setEmail(vendor.getEmail());
			v.setFirstName(vendor.getFirstName());
			v.setLastName(vendor.getLastName());
			v.setPhoneNumber(vendor.getPhoneNumber());
			v.setCreatedOn(new Date());
			v.setCreatedBy(vendorId);

			// Getting site Object
			Site s = session.getObject(Site.class, VendorContantsUtil.SITE_NAME, vendor.getCompany());

			// checking if site name is already present in DB if not insert else use the
			// value from db
			boolean isSiteExists = false;
			if (s == null) {
				Site site = new Site();
				site.setName(vendor.getCompany().toLowerCase());

				v.setSite(site);
			}

			else {
				isSiteExists = true;
				s.setName(vendor.getCompany().toLowerCase());
				v.setSite(s);
			}

			if(roles.size()==0)
			{
				List<Circle> circleWithRole = new ArrayList<>();
				Roles rol=new Roles();
				rol.setRoleName(Role.ADMINROLE.name());
				rol.setStatus("A");
				Circle circles=new Circle();
				circles.setCircleName("PAN_INDIA");
				
				circleWithRole.add(circles);
				
				rol.setCircles(circleWithRole);
				
				
				roles.add(rol);
			}
			
			v.setRoles(roles);

			vendorRepository.addVendor(session, v);
			//addDefaultTax(session, s.getId(), v.getId());
			if (!isSiteExists) {
				// to insert default of email templates
				String site = v.getSite().getId();
				this.createDefaultEmailTemplates(site, v);
				
				// to insert default dunning configurations for the site
				this.addDefaultDunningConfigsForSite(site);

				// generate api credentials
				 generateAPICredentials(session, site, v.getId());

				// add default category
				 addDefaultCategory(session, site, v.getId());

				this.addDefaultTax(session, site, v.getId());
			}

			session.flush();

			// make a call to webhook client
			ServiceCommunicator.triggerWebHook(baseEventBean, session, Vendor.class, v.getId(), siteId, vendorId,
					Constants.CREATE_VENDOR);

			ccAsnPojo.addClearCode(ClearCodes.CREATE_VENDOR_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			CounterNameEnum.CNTR_ADD_VENDOR_SUCCESS.increment();

		} catch (Exception e) {
			try {
				if (session.isSessionActive())
					session.rollback();
			} catch (Exception rollback) {
				rollback.printStackTrace();
				DappLoggerService.GENERAL_INFO_LOG
						.getLogBuilder(
								"Executing [ " + this.getClass().getName() + "."
										+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
								this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
						.writeLog();

				ccAsnPojo.addClearCode(ClearCodes.CREATE_VENDOR_FAILURE.getValue(), ClearCodeLevel.FAILURE);
				ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
				CounterNameEnum.CNTR_ADD_VENDOR_FAILURE.increment();
			}

			// if
			// (e.getMessage().toLowerCase().contains(VendorContantsUtil.UNIQUE_CONSTRAINT_FALIED))
			// {
			// throw new BaseException(HttpStatus.CONFLICT_409,
			// VendorContantsUtil.EMAIL_ALREADY_EXISTS);
			// }
			// throw new BaseException(HttpStatus.BAD_REQUEST_400,
			// VendorContantsUtil.VENDOR_SAVE_ERROR);

		} finally {
			session.close();
		}

		return getVendorInfo(vendor.getEmail());
	
  public String generateStringFromHtml(String htmlFile) {

		// For reading data from html file

		String pathName = null;
		String path = EmailTemplateConstants.HTML_FILE_PATH;

		String profile = System.getProperty("profile");
		if (profile != null && profile.equals("dev")) {
			pathName = "." + path + htmlFile + ".html";

		} else {
			pathName = ".." + path + htmlFile + ".html";

		}
		BufferedReader in=null;
		StringBuilder contentBuilder = new StringBuilder();
		try {
			 in = new BufferedReader(new FileReader(pathName));
			String str;
			while ((str = in.readLine()) != null) {
				contentBuilder.append(str);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		 finally {
		      if(in != null) {
		        try {
		          in.close();
		        } catch (Exception e) {
		          e.printStackTrace();
		        }
		      }
		    }
		String content = contentBuilder.toString();

		return content;
	}
}

@SuppressWarnings("deprecation")
	public String buildDynamicTemaplte(String template, Vendor v) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		ObjectNode planobj = mapper.createObjectNode();
		planobj.put("name", "${plan.name}");
		planobj.put("planCode", "${plan.planCode}");
		planobj.put("description", "${plan.description}");

		rootNode.set("plan", planobj);

		ObjectNode subobj = mapper.createObjectNode();
		subobj.put("firstName", "${subscriber.firstName}");
		subobj.put("lastName", "${subscriber.lastName}");
		subobj.put("email", "${subscriber.email}");

		subobj.put("additionalAddress", "${subscriber.additionalAddress}");

		rootNode.set("subscriber", subobj);

		ObjectNode addobj = mapper.createObjectNode();
		addobj.put("zip", "${subscriber.address.zip}");
		addobj.put("country", "${subscriber.address.country}");
		addobj.put("address2", "${subscriber.address.address2}");
		addobj.put("phone", "${subscriber.address.phone}");
		addobj.put("address1", "${subscriber.address.address1}");
		addobj.put("state", "${subscriber.address.state}");

		rootNode.put("invoiceId", "${invoiceId}");
		rootNode.put("postedOn", "${postedOn}");
		rootNode.put("poNumber", "${poNumber}");
		rootNode.put("netTerms", "${netTerms}");
		rootNode.put("customerNotes", "${customerNotes}");
		rootNode.put("termsAndConditions", "${termsAndConditions}");

		ObjectNode intervalobj = mapper.createObjectNode();
		intervalobj.put("value", "${plan.intervalUnit.value}");

		planobj.put("intervalUnit", intervalobj);
		subobj.put("address", addobj);

		ObjectNode siteObj = mapper.createObjectNode();
		siteObj.put("companyName", v.getSite().getName());

		ObjectNode PunitAmntObj = mapper.createObjectNode();
		PunitAmntObj.put("unitAmount", "${pricePerBillingPeriod.unitAmount}");

		rootNode.put("pricePerBillingPeriod", PunitAmntObj);

		rootNode.put("site", siteObj);
		rootNode.put("email", v.getEmail());

		rootNode.put("id", "${id}");
		rootNode.put("periodEndDate", "${periodEndDate}");
		rootNode.put("termEndDate", "${termEndDate}");

		String data = null;

		try {
			data = FtlUtilImpl.getInstance().buildPojoObj(template, rootNode);
		}

		catch (Exception e) {
			e.printStackTrace();
		}

		if(data!=null)
		{
			
			return data.replaceAll("\u200B", "");
		}
		
		return null;
	}