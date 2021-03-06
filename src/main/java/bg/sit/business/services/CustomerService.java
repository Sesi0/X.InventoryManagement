/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.sit.business.services;

import bg.sit.business.entities.Customer;
import bg.sit.business.entities.CustomerCard;
import bg.sit.business.entities.Product;
import bg.sit.business.entities.ProductCustomerUserID;
import bg.sit.business.entities.User;
import bg.sit.session.SessionHelper;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

/**
 *
 * @author Dell
 */
public class CustomerService extends BaseService {

    // Add customer to the database
    public Customer addCustomer(String name, String location, String phone) {
        Session session = null;
        Transaction transaction = null;
        Customer newCustomer = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            LOGGER.info("Adding new customer.");
            newCustomer = new Customer();
            newCustomer.setName(name);
            newCustomer.setLocation(location);
            newCustomer.setPhone(phone);
            newCustomer.setUser(session.get(User.class, SessionHelper.getCurrentUser().getId()));
            LOGGER.info("Saving and commiting new customer.");
            session.save(newCustomer);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Adding new customer was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return newCustomer;
    }

    // Get all customers for specific user
    public List<Customer> getCustomers(int userID) {
        Session session = null;
        Transaction transaction = null;
        List<Customer> customers = null;
        try {
            session = sessionFactory.openSession();
            LOGGER.info("Getting customers.");

            String hql = "FROM Customer AS c WHERE c.isDeleted = false";
            if (userID > 0) {
                hql = "SELECT c FROM Customer as c INNER JOIN c.user AS u WHERE c.isDeleted = false AND u.id = :userID";
            }

            Query q = session.createQuery(hql, Customer.class);

            if (userID > 0) {
                q.setParameter("userID", userID);
            }

            customers = q.list();
            LOGGER.info("Successfull gotten customers.");

        } catch (Exception e) {
            LOGGER.warning("Getting customers was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return customers;
    }

    // Get all customers
    public List<Customer> getCustomers() {
        return this.getCustomers(-1);
    }

    // Update customer by customerID
    public Customer updateCustomer(int customerID, String name, String location, String phone) {
        Session session = null;
        Transaction transaction = null;
        Customer editCustomer = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            LOGGER.info("Updating customer with ID: " + customerID);

            editCustomer = (Customer) session.createQuery("FROM Customer AS c WHERE c.isDeleted = false AND c.id = :customerID").setParameter("customerID", customerID).getSingleResult();

            if (editCustomer == null) {
                LOGGER.warning("Coludn't find customer with ID: " + customerID);
                throw new Exception("editCustomer is null! (CustomersService -> updateCustomer)");
            }

            if (name != null && !name.equals("")) {
                editCustomer.setName(name);
            }

            if (location != null && !location.equals("")) {
                editCustomer.setLocation(location);
            }

            if (phone != null && !phone.equals("")) {
                editCustomer.setPhone(phone);
            }

            LOGGER.info("Saving and commiting new customer.");
            session.saveOrUpdate(editCustomer);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Updating customer with ID: " + customerID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return editCustomer;
    }

    // Soft delete customer by customerID
    public boolean deleteCustomer(int customerID) {
        Session session = null;
        Transaction transaction = null;
        boolean isSuccessfull = false;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Customer customer = session.find(Customer.class, customerID);
            customer.setIsDeleted(true);
            session.save(customer);
            LOGGER.info("Saving and commiting soft delete of customer.");
            transaction.commit();
            isSuccessfull = true;
        } catch (Exception e) {
            LOGGER.warning("Soft deleting customer with ID: " + customerID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return isSuccessfull;
    }

    // Force delete customer by customerID from database, once deleted it cannot be reverted
    public boolean forceDeleteCustomer(int customerID) {
        Session session = null;
        Transaction transaction = null;
        boolean isSuccessfull = false;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Customer customer = session.find(Customer.class, customerID);
            session.delete(customer);
            LOGGER.info("Saving and commiting force delete of customer.");
            transaction.commit();
            isSuccessfull = true;
        } catch (Exception e) {
            LOGGER.warning("Force deleting customer with ID: " + customerID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return isSuccessfull;
    }

    // Add customer card to the database
    public CustomerCard addCustomerCard(int customerID, int productID) {
        Session session = null;
        Transaction transaction = null;
        CustomerCard newCustomerCard = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            LOGGER.info("Adding customer card.");
            newCustomerCard = new CustomerCard();

            Product chosenProduct = session.get(Product.class, productID);

            if (chosenProduct == null) {
                LOGGER.warning("Coludn't find product with ID: " + customerID);
                throw new Exception("chosenProduct is null. (CustomerService => addCustomerCard)");
            }

            chosenProduct.addCustomerCard(newCustomerCard);
            chosenProduct.setIsAvailable(false);

            Customer chosenCustomer = session.get(Customer.class, customerID);

            if (chosenCustomer == null) {
                LOGGER.warning("Coludn't find customer with ID: " + customerID);
                throw new Exception("chosenCustomer is null. (CustomerService => addCustomerCard)");
            }

            chosenCustomer.addCustomerCard(newCustomerCard);

            User chosenUser = session.get(User.class, SessionHelper.getCurrentUser().getId());

            if (chosenUser == null) {
                LOGGER.warning("Coludn't find user with ID: " + SessionHelper.getCurrentUser().getId());
                throw new Exception("chosenUser is null. (CustomerService => addCustomerCard)");
            }

            chosenUser.addCustomerCard(newCustomerCard);

            newCustomerCard.setDateBorrowed(SessionHelper.getCurrentDate());
            newCustomerCard.setId(new ProductCustomerUserID(chosenProduct.getId(), chosenCustomer.getId(), chosenUser.getId()));
            LOGGER.info("Saving and commiting adding customer card.");
            session.persist(newCustomerCard);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Adding customer card was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return newCustomerCard;
    }

    // Soft delete customer by customerID
    public boolean removeCustomerCard(int customerID, int productID) {
        Session session = null;
        Transaction transaction = null;
        boolean isSuccessfull = false;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            LOGGER.info("Removing customer card.");
            CustomerCard customer = session.createQuery("FROM CustomerCard WHERE isDeleted = false AND product.id = " + productID + " AND customer.id = " + customerID, CustomerCard.class).getSingleResult();
            LOGGER.info("Saving and commiting removing customer card.");
            session.remove(customer);
            transaction.commit();
            isSuccessfull = true;
        } catch (Exception e) {
            LOGGER.warning("Removing customer card was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return isSuccessfull;
    }
}
