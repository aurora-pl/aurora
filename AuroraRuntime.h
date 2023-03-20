//
// Created by snwy on 3/3/23.
//

#ifndef AURORARS_AURORARUNTIME_H
#define AURORARS_AURORARUNTIME_H

#include <utility>
#include <vector>
#include <iostream>
#include <cmath>
#include <memory_resource>
#include <cstring>

int CURRENT_LINE = 0;

void error(const std::string& message) {
    std::cout << "at line " << CURRENT_LINE << ": " << message << std::endl;
    exit(1);
}

struct AuroraObject;

typedef void(*AuroraSub)(void*, int argc, AuroraObject* argv, AuroraObject& self);
typedef AuroraObject(*AuroraFunc)(void*, int argc, AuroraObject* argv, AuroraObject& self);

struct __attribute__ ((packed)) AuroraObject {
    enum {
        Str,
        Num,
        Bool,
        List,
        Sub,
        Func,
    } type;
    union {
        std::string str;
        double num;
        std::vector<AuroraObject> list;
        AuroraSub sub;
        AuroraFunc func;
        bool boolean;
    };

    void* lambda_ptr;

    struct str_type {};

    explicit AuroraObject(std::string str, str_type) : type(Str), str(std::move(str)) {}
    explicit AuroraObject(double num) : type(Num), num(num) {}
    explicit AuroraObject(bool boolean) : type(Bool), boolean(boolean) {}
    explicit AuroraObject(std::vector<AuroraObject>  list) : type(List), list(std::move(list)) {}
    explicit AuroraObject(AuroraSub sub, void* lambda_ptr) : type(Sub), sub(sub), lambda_ptr(lambda_ptr) {}
    explicit AuroraObject(AuroraFunc func, void* lambda_ptr) : type(Func), func(func), lambda_ptr(lambda_ptr) {}
    AuroraObject() : type(Bool), boolean(false) {}

    AuroraObject(const AuroraObject& other) : AuroraObject() {
        if(other.type == Str || other.type == List) {
            *this = other;
        } else {
            memcpy(this, &other, sizeof(AuroraObject));
        }
    }

    AuroraObject(AuroraObject&& other)  noexcept : AuroraObject() {
            memcpy(this, &other, sizeof(AuroraObject));
            other.type = Bool;
    }

    ~AuroraObject() {
        if (type == List) {
            std::vector<AuroraObject>().swap(list);
        } else if (type == Str) {
            str.~basic_string();
        }
    }

    AuroraObject & operator=(AuroraObject&& other)  noexcept {
        memcpy(this, &other, sizeof(AuroraObject));
        other.type = Bool;
    }

    AuroraObject & operator=(const AuroraObject& other) {
        if(this == &other)
            return *this;
        if (type == List) {
            list.~vector();
        } else if (type == Str) {
            str.~basic_string();
        }
        type = other.type;
        switch (type) {
            case Str: {
                new (&str) std::string(other.str);
                break;
            }
            case Num:
                num = other.num;
                break;
            case Bool:
                boolean = other.boolean;
                break;
            case List:
                new (&list) std::vector<AuroraObject>(other.list);
                break;
            case Sub:
                sub = other.sub;
                break;
            case Func:
                func = other.func;
                break;
        }
        return *this;
    }

    AuroraObject & operator=(double other) {
        if (type == List) {
            list.~vector();
        } else if (type == Str) {
            str.~basic_string();
        }
        type = Num;
        num = other;
        return *this;
    }

    std::string toString() const {
        switch (type) {
            case Str:
                return str;
            case Num:
                return std::to_string(num);
            case Bool:
                return boolean ? "true" : "false";
            case List: {
                std::string result = "{";
                for (int i = 0; i < list.size(); i++) {
                    result += list[i].toString();
                    if (i != list.size() - 1) {
                        result += ", ";
                    }
                }
                result += "}";
                return result;
            }
            case Sub:
                return "sub";
            case Func:
                return "func";
        }
    }

    AuroraObject operator+(const AuroraObject& other) const {
        switch(type) {
            case Num:
                if (other.type == Num) {
                    return AuroraObject(num + other.num);
                } else {
                    error("invalid types for + operator");
                }
            case Str:
                if (other.type == Str) {
                    return AuroraObject(str + other.str, str_type{});
                } else {
                    error("invalid types for + operator");
                }
            case List:
                if (other.type == List) {
                    std::vector<AuroraObject> newList = list;
                    newList.insert(newList.end(), other.list.begin(), other.list.end());
                    return AuroraObject(newList);
                } else {
                    error("invalid types for + operator");
                }
            default:
                error("invalid types for + operator");
        }
    }

    AuroraObject operator+(double other) const {
        if (type == Num) {
            return AuroraObject(num + other);
        } else {
            error("invalid types for + operator");
        }
    }

    AuroraObject operator+=(const AuroraObject& other) {
        switch(type) {
            case Num:
                if (other.type == Num) {
                    num += other.num;
                    return *this;
                } else {
                    error("invalid types for += operator");
                }
            case Str:
                if (other.type == Str) {
                    str += other.str;
                    return *this;
                } else {
                    error("invalid types for += operator");
                }
            case List:
                if (other.type == List) {
                    list.insert(list.end(), other.list.begin(), other.list.end());
                    return *this;
                } else {
                    error("invalid types for += operator");
                }
            default:
                error("invalid types for += operator");
        }
    }

    AuroraObject operator+=(double other) {
        if (type == Num) {
            num += other;
            return *this;
        } else {
            error("invalid types for += operator");
        }
    }

    AuroraObject operator-=(const AuroraObject& other) {
        if (type == Num && other.type == Num) {
            num -= other.num;
            return *this;
        } else {
            error("invalid types for -= operator");
        }
    }

    AuroraObject operator-=(double other) {
        if (type == Num) {
            num -= other;
            return *this;
        } else {
            error("invalid types for -= operator");
        }
    }

    AuroraObject operator*=(const AuroraObject& other) {
        if (type == Num && other.type == Num) {
            num *= other.num;
            return *this;
        } else {
            error("invalid types for *= operator");
        }
    }

    AuroraObject operator*=(double other) {
        if (type == Num) {
            num *= other;
            return *this;
        } else {
            error("invalid types for *= operator");
        }
    }

    AuroraObject operator/=(const AuroraObject& other) {
        if (type == Num && other.type == Num) {
            num /= other.num;
            return *this;
        } else {
            error("invalid types for /= operator");
        }
    }

    AuroraObject operator/=(double other) {
        if (type == Num) {
            num /= other;
            return *this;
        } else {
            error("invalid types for /= operator");
        }
    }

    AuroraObject operator%=(const AuroraObject& other) {
        if (type == Num && other.type == Num) {
            num = fmod(num, other.num);
            return *this;
        } else {
            error("invalid types for %= operator");
        }
    }

    AuroraObject operator%=(double other) {
        if (type == Num) {
            num = fmod(num, other);
            return *this;
        } else {
            error("invalid types for %= operator");
        }
    }

    AuroraObject operator-(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num - other.num);
        } else {
            error("invalid types for - operator");
        }
    }

    AuroraObject operator-(double other) const {
        if (type == Num) {
            return AuroraObject(num - other);
        } else {
            error("invalid types for - operator");
        }
    }

    AuroraObject operator*(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num * other.num);
        } else {
            error("invalid types for * operator");
        }
    }

    AuroraObject operator*(double other) const {
        if (type == Num) {
            return AuroraObject(num * other);
        } else {
            error("invalid types for * operator");
        }
    }

    AuroraObject operator/(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num / other.num);
        } else {
            error("invalid types for / operator");
        }
    }

    AuroraObject operator/(double other) const {
        if (type == Num) {
            return AuroraObject(num / other);
        } else {
            error("invalid types for / operator");
        }
    }

    AuroraObject operator%(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(fmod(num, other.num));
        } else {
            error("invalid types for % operator");
        }
    }

    AuroraObject operator%(double other) const {
        if (type == Num) {
            return AuroraObject(fmod(num, other));
        } else {
            error("invalid types for % operator");
        }
    }

    AuroraObject operator==(const AuroraObject& other) const {
        switch (type) {
            case Str:
                if (other.type == Str) {
                    return AuroraObject(str == other.str);
                } else {
                    return AuroraObject(false);
                }
            case Num:
                if (other.type == Num) {
                    return AuroraObject(num == other.num);
                } else {
                    return AuroraObject(false);
                }
            case Bool:
                if (other.type == Bool) {
                    return AuroraObject(boolean == other.boolean);
                } else {
                    return AuroraObject(false);
                }
            case List:
                if (other.type == List) {
                    return AuroraObject(list == other.list);
                } else {
                    return AuroraObject(false);
                }
            default:
                error("invalid types for equality operator");
        }
    }

    AuroraObject operator==(double other) const {
        if (type == Num) {
            return AuroraObject(num == other);
        } else {
            return AuroraObject(false);
        }
    }

    AuroraObject operator!=(const AuroraObject& other) const {
        return !(*this == other);
    }

    AuroraObject operator!=(double other) const {
        return !(*this == other);
    }

    AuroraObject operator>(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num > other.num);
        } else if (type == Str && other.type == Str) {
            return AuroraObject(str > other.str);
        } else {
            error("invalid types for > operator");
        }
    }

    AuroraObject operator>(double other) const {
        if (type == Num) {
            return AuroraObject(num > other);
        } else {
            error("invalid types for > operator");
        }
    }

    AuroraObject operator<(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num < other.num);
        } else if (type == Str && other.type == Str) {
            return AuroraObject(str < other.str);
        } else {
            error("invalid types for < operator");
        }
    }

    AuroraObject operator<(double other) const {
        if (type == Num) {
            return AuroraObject(num < other);
        } else {
            error("invalid types for < operator");
        }
    }

    AuroraObject operator>=(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num >= other.num);
        } else if (type == Str && other.type == Str) {
            return AuroraObject(str >= other.str);
        } else {
            error("invalid types for >= operator");
        }
    }

    AuroraObject operator>=(double other) const {
        if (type == Num) {
            return AuroraObject(num >= other);
        } else {
            error("invalid types for >= operator");
        }
    }

    AuroraObject operator<=(const AuroraObject& other) const {
        if (type == Num && other.type == Num) {
            return AuroraObject(num <= other.num);
        } else if (type == Str && other.type == Str) {
            return AuroraObject(str <= other.str);
        } else {
            error("invalid types for <= operator");
        }
    }

    AuroraObject operator<=(double other) const {
        if (type == Num) {
            return AuroraObject(num <= other);
        } else {
            error("invalid types for <= operator");
        }
    }

    AuroraObject operator!() const {
        if (type == Bool) {
            return AuroraObject(!boolean);
        } else {
            error("invalid type for 'not' operator");
        }
    }

    AuroraObject operator&&(const AuroraObject& other) const {
        if (type == Bool && other.type == Bool) {
            return AuroraObject(boolean && other.boolean);
        } else {
            error("invalid types for 'and' operator");
        }
    }

    AuroraObject operator||(const AuroraObject& other) const {
        if (type == Bool && other.type == Bool) {
            return AuroraObject(boolean || other.boolean);
        } else {
            error("invalid types for 'or' operator");
        }
    }

    AuroraObject operator[](const AuroraObject& other) {
        if (type == List && other.type == Num) {
            return list[(int) other.num];
        } else {
            error("invalid types for : operator");
        }
    }

    AuroraObject operator[](double other) {
        if (type == List) {
            return list[(int) other];
        } else {
            error("invalid types for : operator");
        }
    }

    void set(double index, const AuroraObject& other) {
        if (type == List) {
            list.at((int) index) = other;
        } else {
            error("invalid types for : operator");
        }
    }

    AuroraObject operator()(int argc, AuroraObject* argv, bool onlyFunc) const {
        if (type == Func) {
            return func(lambda_ptr, argc, argv, const_cast<AuroraObject&>(*this));
        } else if(type == Sub) {
            if (onlyFunc) {
                error("cannot call subroutine in expression");
            }
            sub(lambda_ptr, argc, argv, const_cast<AuroraObject&>(*this));
            return {};
        }
        std::cout << type << std::endl;
        error("invalid type for call");
    }

    explicit operator bool() const {
        if (type == Bool) {
            return boolean;
        } else {
            error("invalid type for condition");
        }
    }
};

void print_fn(void* _, int argc, AuroraObject* argv, const AuroraObject& __) {
    for (int i = 0; i < argc; i++) {
        std::cout << argv[i].toString() << " ";
    }
    std::cout << std::endl;
}

auto const print = AuroraObject(AuroraSub(print_fn), nullptr);

#endif //AURORARS_AURORARUNTIME_H
