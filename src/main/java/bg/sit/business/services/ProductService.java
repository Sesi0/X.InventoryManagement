/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.sit.business.services;

import bg.sit.business.constants.Constants;
import bg.sit.business.entities.Amortization;
import bg.sit.business.entities.DiscardedProduct;
import bg.sit.business.entities.Product;
import bg.sit.business.entities.ProductType;
import bg.sit.business.entities.User;
import bg.sit.session.SessionHelper;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author Dell
 */
public class ProductService extends BaseService {

// Add product to the database
    public Product addProduct(int productTypeID, double price, int amortizationID) {
        Session session = null;
        Transaction transaction = null;
        Product newProduct = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            LOGGER.info("Adding new product.");
            Integer lastID;
            lastID = (Integer) (session.createQuery("SELECT p.id FROM Product AS p ORDER BY p.id DESC").setMaxResults(1).uniqueResult());
            if (lastID == null) {
                lastID = 0;
            }

            newProduct = new Product();
            ProductType chosenProductType = session.get(ProductType.class, productTypeID);

            if (chosenProductType == null) {
                LOGGER.warning("Coludn't find product type with ID: " + productTypeID);
                throw new Exception("Could not find product type with ID " + productTypeID);
            }

            chosenProductType.addProduct(newProduct);

            if (amortizationID > 0 && price > Constants.DEFAULT_MA_LIMIT) {
                newProduct.setIsDMA(true);
                Amortization chosenAmortization = session.get(Amortization.class, amortizationID);
                if (chosenAmortization == null) {
                    LOGGER.warning("Coludn't find amortization with ID: " + amortizationID);
                    throw new Exception("Could not find amortization with ID " + amortizationID);
                }

                chosenAmortization.addProduct(newProduct);
            }

            newProduct.setIsAvailable(true);
            newProduct.setPrice(price);
            newProduct.setDateCreated(SessionHelper.getCurrentDate());
            newProduct.setUser(session.get(User.class, SessionHelper.getCurrentUser().getId()));

            newProduct.setInventoryNumber("U" + newProduct.getUser().getId() + "-PT" + newProduct.getProductType().getId() + "-" + (lastID + 1));
            LOGGER.info("Saving and commiting adding product.");
            session.save(newProduct);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Adding product was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return newProduct;
    }

    // Get all product for specific user
    public List<Product> getProducts(int userID) {
        Session session = null;
        Transaction transaction = null;
        List<Product> products = null;
        try {
            session = sessionFactory.openSession();
            LOGGER.info("Getting products.");

            String hql = "FROM Product AS p WHERE p.isDeleted = false";
            if (userID > 0) {
                hql += " AND p.user.id = " + userID;
            }

            products = session.createQuery(hql, Product.class).list();
            LOGGER.info("Successfull gotten products.");
        } catch (Exception e) {
            LOGGER.warning("Getting products was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return products;
    }

    // Get all product
    public List<Product> getProducts() {
        return this.getProducts(-1);
    }

    // Update product by productID
    public Product updateProduct(int productID, double price, int amortizationID) {
        Session session = null;
        Transaction transaction = null;
        Product editProduct = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            LOGGER.info("Updating product with ID: " + productID);

            editProduct = (Product) session.createQuery("FROM Product AS p WHERE p.isDeleted = false AND p.id = :productID").setParameter("productID", productID).getSingleResult();

            if (editProduct == null) {
                LOGGER.warning("Coludn't find product with ID: " + productID);
                throw new Exception("editProduct is null! (ProductService -> updateProduct)");
            }

            if (price > 0) {
                editProduct.setPrice(price);
            }

            if (amortizationID > 0 && editProduct.getAmortization() != null) {
                Amortization oldAmortization = editProduct.getAmortization();
                Amortization newAmortization = session.get(Amortization.class, amortizationID);

                if (oldAmortization == null || newAmortization == null) {
                    LOGGER.warning("oldAmortization or newAmortization is null!");
                    throw new Exception("oldAmortization or newAmortization is null! (ProductService -> updateProduct)");
                }

                if (oldAmortization.getId() != newAmortization.getId()) {
                    oldAmortization.removeProduct(editProduct);
                    newAmortization.addProduct(editProduct);
                }
            }

            session.update(editProduct);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Updating product with ID " + productID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return editProduct;
    }

    // Soft delete product by productID
    public boolean deleteProduct(int productID) {
        Session session = null;
        Transaction transaction = null;
        boolean isSuccessfull = false;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Product product = session.find(Product.class, productID);
            product.setIsDeleted(true);
            LOGGER.info("Saving and commiting soft delete of product.");
            session.save(product);
            transaction.commit();
            isSuccessfull = true;
        } catch (Exception e) {
            LOGGER.warning("Soft deleting product with ID: " + productID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return isSuccessfull;
    }

    // Force delete product by productID from database, once deleted it cannot be reverted
    public boolean forceDeleteProduct(int productID) {
        Session session = null;
        Transaction transaction = null;
        boolean isSuccessfull = false;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Product product = session.find(Product.class, productID);
            LOGGER.info("Saving and commiting force delete of product.");
            session.delete(product);
            transaction.commit();
            isSuccessfull = true;
        } catch (Exception e) {
            LOGGER.warning("Force deleting product with ID: " + productID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return isSuccessfull;
    }

    public DiscardedProduct discardProduct(int productID, String reason) {
        Session session = null;
        Transaction transaction = null;
        DiscardedProduct newDiscardedProduct = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            newDiscardedProduct = new DiscardedProduct();
            Product chosenProduct = session.get(Product.class, productID);

            if (chosenProduct == null) {
                LOGGER.warning("Coludn't find product with ID: " + productID);
                throw new Exception("Could not find product with ID " + productID);
            }

            chosenProduct.setIsAvailable(false);
            newDiscardedProduct.setProduct(chosenProduct);
            newDiscardedProduct.setUser(session.get(User.class, SessionHelper.getCurrentUser().getId()));
            newDiscardedProduct.setDateDiscarded(SessionHelper.getCurrentDate());
            newDiscardedProduct.setReason(reason);
            LOGGER.info("Saving and commiting discard of product.");
            session.save(newDiscardedProduct);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Discarding product with ID: " + productID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return newDiscardedProduct;
    }

    public Product getProductForDiscard() {
        Session session = null;
        Product productForDiscard = null;

        try {
            session = sessionFactory.openSession();

            long time = SessionHelper.getCurrentDate().getTime() - Duration.ofDays(365 * SessionHelper.getYearsBeforeDiscard()).toMillis();
            productForDiscard = (Product) session.createQuery("SELECT p FROM Product AS p LEFT JOIN p.discardedProduct AS dp WHERE p.isDeleted = false AND dp IS NULL AND p.dateCreated <= :time")
                    .setParameter("time", new Timestamp(time))
                    .setMaxResults(1)
                    .getSingleResult();

            if (productForDiscard == null) {
                throw new Exception("productForDiscard is null! (ProductService -> getProductForDiscard)");
            }
        } catch (Exception e) {
            LOGGER.warning("Getting product with for discard was unsuccessfull:\n" + e.getStackTrace());
        } finally {
            session.close();
        }

        return productForDiscard;
    }

    // Get all product for specific user inverted means all products but specified customer
    public List<Product> getProductsByCustomer(int customerID, boolean inverted) {
        Session session = null;
        Transaction transaction = null;
        List<Product> products = null;
        try {
            session = sessionFactory.openSession();

            String hql = "SELECT p FROM Product p, CustomerCard cc LEFT JOIN p.discardedProduct dp WHERE cc.isDeleted = false AND p.isDeleted = false AND dp IS NULL AND cc in elements(p.customerCards) AND cc.customer.id = " + customerID;

            if (inverted) {
                hql = "SELECT p FROM Product AS p LEFT JOIN p.discardedProduct AS dp WHERE p.isDeleted = false AND dp IS NULL AND size(p.customerCards) = 0";
            }

            products = session.createQuery(hql, Product.class).list();
        } catch (Exception e) {
            LOGGER.warning("Getting products for customer with ID: " + customerID + " was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return products;
    }

    public List<Product> updateProductPricesForAmortizations() {
        Session session = null;
        Transaction transaction = null;
        List<Product> maProducts = new ArrayList<Product>();

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            List<Product> products = null;

            products = session.createQuery("SELECT p FROM Product AS p LEFT JOIN p.discardedProduct AS pdp RIGHT JOIN p.amortization AS pa WHERE p.isDeleted = false AND p.isDMA = true AND pdp IS NULL AND pa IS NOT NULL AND p.amortizationRepeatedTimes < pa.repeatLimit", Product.class).list();

            if (products == null || products.isEmpty()) {
                LOGGER.info("Products for amortization prices updates are empty.");
                return maProducts;
            }

            for (Product product : products) {
                Amortization amortization = product.getAmortization();

                Date pDate = product.getDateCreated();
                long daysAfterCreated = TimeUnit.DAYS.convert(SessionHelper.getCurrentDate().getTime() - pDate.getTime(), TimeUnit.MILLISECONDS);
                int daysToCheck = amortization.getDays();

                if (product.getAmortizationRepeatedTimes() > 0) {
                    daysToCheck *= product.getAmortizationRepeatedTimes();
                }

                if (daysAfterCreated >= daysToCheck) {
                    product.setPrice(product.getPrice() - amortization.getPrice());
                    product.setAmortizationRepeatedTimes(product.getAmortizationRepeatedTimes() + 1);

                    if (product.getPrice() < SessionHelper.getMaLimit()) {
                        product.setIsDMA(false);
                        product.setAmortization(null);
                        maProducts.add(product);
                    }
                }

            }

            transaction.commit();
        } catch (Exception e) {
            LOGGER.warning("Updating products prices by amortization settings was unsuccessfull:\n" + e.getStackTrace());
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }

        return maProducts;
    }
}
