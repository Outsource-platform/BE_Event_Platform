package org.example.eventplatform.identity.service;

import org.example.eventplatform.identity.entity.User;

/**
 * Tài khoản nhận tiền của thành viên. Để trống cả ba ô là xoá. Nhập dở thì báo, kẻo trưởng đoàn
 * chuyển khoản thiếu tên ngân hàng hoặc chủ tài khoản.
 */
public final class BankAccounts {

    private BankAccounts() {
    }

    public static void apply(User user, String bankName, String accountNumber, String accountHolder) {
        String name = blankToNull(bankName);
        String number = blankToNull(accountNumber);
        String holder = blankToNull(accountHolder);
        boolean any = name != null || number != null || holder != null;
        if (any && (name == null || number == null || holder == null)) {
            throw new IllegalArgumentException("Nhập đủ ngân hàng, số tài khoản và chủ tài khoản");
        }
        if (number != null) {
            String digits = number.replaceAll("\\s", "");
            if (!digits.matches("\\d{6,20}")) {
                throw new IllegalArgumentException("Số tài khoản chỉ gồm 6 đến 20 chữ số");
            }
            number = digits;
        }
        user.setBankName(name);
        user.setBankAccountNumber(number);
        user.setBankAccountHolder(holder);
    }

    public static void clear(User user) {
        user.setBankName(null);
        user.setBankAccountNumber(null);
        user.setBankAccountHolder(null);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
