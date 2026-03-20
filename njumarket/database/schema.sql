--
-- PostgreSQL database dump
--

\restrict AKE2LVKKSiYr4NszPHsPFzia2eehc6TK2DBR54YMNHpogxPax09O8fAlJCC6Jru

-- Dumped from database version 16.11 (Debian 16.11-1.pgdg11+1)
-- Dumped by pg_dump version 18.1

-- Started on 2026-03-10 11:59:02

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 23 (class 2615 OID 21712)
-- Name: nju_market; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA nju_market;


ALTER SCHEMA nju_market OWNER TO postgres;

--
-- TOC entry 1316 (class 1255 OID 22536)
-- Name: update_commodity_vector_updated_at(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_commodity_vector_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_commodity_vector_updated_at() OWNER TO postgres;

--
-- TOC entry 1315 (class 1255 OID 22183)
-- Name: update_user_addresses_updated_time(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_user_addresses_updated_time() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_user_addresses_updated_time() OWNER TO postgres;

--
-- TOC entry 1317 (class 1255 OID 22577)
-- Name: update_user_profile_vector_updated_at(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_user_profile_vector_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_user_profile_vector_updated_at() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 326 (class 1259 OID 21724)
-- Name: admins; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.admins (
    admin_id character varying(50) NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    real_name character varying(50),
    email character varying(100),
    department character varying(50),
    "position" character varying(50),
    admin_level character varying(20) DEFAULT 'administrator'::character varying NOT NULL,
    permissions text,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    last_login_time timestamp with time zone,
    last_login_ip character varying(50),
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    login_count integer DEFAULT 0 NOT NULL,
    remark character varying(500)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.admins OWNER TO postgres;

--
-- TOC entry 5192 (class 0 OID 0)
-- Dependencies: 326
-- Name: TABLE admins; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.admins IS '管理员表 - 存储内部管理员账号信息';


--
-- TOC entry 5193 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.admin_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.admin_id IS '管理员ID';


--
-- TOC entry 5194 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.username; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.username IS '用户名';


--
-- TOC entry 5195 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.password; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.password IS '密码（加密存储）';


--
-- TOC entry 5196 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.real_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.real_name IS '真实姓名';


--
-- TOC entry 5197 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.email; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.email IS '邮箱';


--
-- TOC entry 5198 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.department; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.department IS '部门';


--
-- TOC entry 5199 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins."position"; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins."position" IS '职位';


--
-- TOC entry 5200 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.admin_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.admin_level IS '管理员级别：system-系统管理员，administrator-普通管理员';


--
-- TOC entry 5201 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.permissions; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.permissions IS '权限列表（JSON格式）';


--
-- TOC entry 5202 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.create_time IS '创建时间';


--
-- TOC entry 5203 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.update_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.update_time IS '更新时间';


--
-- TOC entry 5204 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.last_login_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.last_login_time IS '最后登录时间';


--
-- TOC entry 5205 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.last_login_ip; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.last_login_ip IS '最后登录IP';


--
-- TOC entry 5206 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.account_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.account_status IS '账户状态：ACTIVE-活跃，SUSPENDED-暂停，BANNED-禁用';


--
-- TOC entry 5207 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.login_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.login_count IS '登录次数';


--
-- TOC entry 5208 (class 0 OID 0)
-- Dependencies: 326
-- Name: COLUMN admins.remark; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.remark IS '备注';


--
-- TOC entry 327 (class 1259 OID 21745)
-- Name: commodities; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodities (
    commodity_id character varying(50) NOT NULL,
    seller_id character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    price double precision NOT NULL,
    stock integer NOT NULL,
    location character varying(200),
    category character varying(50),
    condition_level character varying(20) DEFAULT 'GOOD'::character varying,
    images text,
    publish_time timestamp with time zone,
    commodity_status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    buyer_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    click_count integer DEFAULT 0 NOT NULL,
    report_count integer DEFAULT 0 NOT NULL,
    address_id character varying(50),
    address_snapshot_province character varying(50),
    address_snapshot_city character varying(50),
    address_snapshot_district character varying(50),
    address_snapshot_street character varying(200),
    address_snapshot_detail character varying(500),
    address_snapshot_full text,
    location_geography public.geography(Point,4326),
    longitude double precision,
    latitude double precision
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodities OWNER TO postgres;

--
-- TOC entry 5219 (class 0 OID 0)
-- Dependencies: 327
-- Name: TABLE commodities; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.commodities IS '商品表';


--
-- TOC entry 5220 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.commodity_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.commodity_id IS '商品ID';


--
-- TOC entry 5221 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.seller_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.seller_id IS '卖家用户ID';


--
-- TOC entry 5222 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.title; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.title IS '商品标题';


--
-- TOC entry 5223 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.description IS '商品描述';


--
-- TOC entry 5224 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.stock; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.stock IS '库存数量';


--
-- TOC entry 5225 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.location; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.location IS '商品位置';


--
-- TOC entry 5226 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.category; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.category IS '商品分类';


--
-- TOC entry 5227 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.condition_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.condition_level IS '商品成色: EXCELLENT-优秀, GOOD-良好, FAIR-一般, POOR-较差';


--
-- TOC entry 5228 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.images; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.images IS '商品图片URL列表(JSON格式)';


--
-- TOC entry 5229 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.commodity_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.commodity_status IS '商品状态: DRAFT-草稿, PUBLISHED-已发布, SOLD_OUT-售罄, REMOVED-已下架';


--
-- TOC entry 5230 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.buyer_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.buyer_visibility IS '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- TOC entry 5231 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.click_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.click_count IS '点击次数';


--
-- TOC entry 5232 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.report_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.report_count IS '举报次数';


--
-- TOC entry 5233 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_id IS '商品地址ID（引用user_addresses表）';


--
-- TOC entry 5234 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_snapshot_province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_province IS '地址快照-省份';


--
-- TOC entry 5235 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_snapshot_city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_city IS '地址快照-城市';


--
-- TOC entry 5236 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_snapshot_district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_district IS '地址快照-区/县';


--
-- TOC entry 5237 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_snapshot_street; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_street IS '地址快照-街道';


--
-- TOC entry 5238 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_snapshot_detail; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_detail IS '地址快照-详细地址';


--
-- TOC entry 5239 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.address_snapshot_full; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_full IS '地址快照-完整地址';


--
-- TOC entry 5240 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.location_geography; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.location_geography IS '地理位置（PostGIS Geography类型）';


--
-- TOC entry 5241 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.longitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.longitude IS '经度';


--
-- TOC entry 5242 (class 0 OID 0)
-- Dependencies: 327
-- Name: COLUMN commodities.latitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.latitude IS '纬度';


--
-- TOC entry (class 1259) - 商品库存表（归属订单服务，超卖防护在订单服务本地完成）
-- Name: commodity_inventory; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodity_inventory (
    commodity_id character varying(50) NOT NULL,
    available_quantity integer NOT NULL DEFAULT 0,
    total_quantity integer NOT NULL DEFAULT 0,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodity_inventory OWNER TO postgres;

COMMENT ON TABLE nju_market.commodity_inventory IS '商品库存表（订单服务本地管理，与 commodities.stock 同步；不设外键跨服务）';
COMMENT ON COLUMN nju_market.commodity_inventory.commodity_id IS '商品ID，与 commodities.commodity_id 一一对应';
COMMENT ON COLUMN nju_market.commodity_inventory.available_quantity IS '当前可用库存（下单扣减用）';
COMMENT ON COLUMN nju_market.commodity_inventory.total_quantity IS '卖家设定总库存（来自商品服务同步）';
COMMENT ON COLUMN nju_market.commodity_inventory.updated_at IS '更新时间';


ALTER TABLE ONLY nju_market.commodity_inventory
    ADD CONSTRAINT commodity_inventory_pkey PRIMARY KEY (commodity_id);


--
-- TOC entry 328 (class 1259 OID 21756)
-- Name: commodity_categories; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodity_categories (
    category_id character varying(50) NOT NULL,
    category_name character varying(100) NOT NULL,
    parent_id character varying(50),
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodity_categories OWNER TO postgres;

--
-- TOC entry 5243 (class 0 OID 0)
-- Dependencies: 328
-- Name: TABLE commodity_categories; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.commodity_categories IS '商品分类表';


--
-- TOC entry 5244 (class 0 OID 0)
-- Dependencies: 328
-- Name: COLUMN commodity_categories.category_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.category_id IS '分类ID';


--
-- TOC entry 5245 (class 0 OID 0)
-- Dependencies: 328
-- Name: COLUMN commodity_categories.category_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.category_name IS '分类名称';


--
-- TOC entry 5246 (class 0 OID 0)
-- Dependencies: 328
-- Name: COLUMN commodity_categories.parent_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.parent_id IS '父分类ID';


--
-- TOC entry 5247 (class 0 OID 0)
-- Dependencies: 328
-- Name: COLUMN commodity_categories.sort_order; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.sort_order IS '排序';


--
-- TOC entry 5248 (class 0 OID 0)
-- Dependencies: 328
-- Name: COLUMN commodity_categories.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.is_active IS '是否启用';


--
-- TOC entry 5249 (class 0 OID 0)
-- Dependencies: 328
-- Name: COLUMN commodity_categories.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.create_time IS '创建时间';


--
-- TOC entry 329 (class 1259 OID 21761)
-- Name: commodity_snapshots; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodity_snapshots (
    snapshot_id character varying(50) NOT NULL,
    -- 旧版字段（历史快照信息，当前代码未使用但保留以兼容已有数据）
    category character varying(50),
    commodity_status character varying(20),
    condition_level character varying(20),
    description text,
    images text,
    location character varying(200),
    original_commodity_id character varying(50),
    price double precision NOT NULL,
    seller_email character varying(100),
    seller_id character varying(50),
    seller_name character varying(100),
    seller_phone character varying(20),
    snapshot_time timestamp(6) with time zone,
    stock integer,
    title character varying(200) NOT NULL,
    -- 新增字段：与 message-service 当前实体 CommoditySnapshot 对齐
    -- 每条 COMMODITY_CARD 类型消息对应一条唯一快照记录
    message_id character varying(255),
    commodity_id character varying(50),
    image_url character varying(500),
    status character varying(20)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodity_snapshots OWNER TO postgres;

--
-- TOC entry 331 (class 1259 OID 21773)
-- Name: contact_blacklist; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.contact_blacklist (
    id bigint NOT NULL,
    user_id character varying(50) NOT NULL,
    blocked_user_id character varying(50) NOT NULL,
    reason character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
)
WITH (fillfactor='90');


ALTER TABLE nju_market.contact_blacklist OWNER TO postgres;

--
-- TOC entry 5250 (class 0 OID 0)
-- Dependencies: 331
-- Name: TABLE contact_blacklist; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.contact_blacklist IS '联系人黑名单表';


--
-- TOC entry 5251 (class 0 OID 0)
-- Dependencies: 331
-- Name: COLUMN contact_blacklist.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.user_id IS '用户ID';


--
-- TOC entry 5252 (class 0 OID 0)
-- Dependencies: 331
-- Name: COLUMN contact_blacklist.blocked_user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.blocked_user_id IS '被屏蔽用户ID';


--
-- TOC entry 5253 (class 0 OID 0)
-- Dependencies: 331
-- Name: COLUMN contact_blacklist.reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.reason IS '屏蔽原因';


--
-- TOC entry 5254 (class 0 OID 0)
-- Dependencies: 331
-- Name: COLUMN contact_blacklist.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.created_at IS '屏蔽时间';


--
-- TOC entry 330 (class 1259 OID 21772)
-- Name: contact_blacklist_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.contact_blacklist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.contact_blacklist_id_seq OWNER TO postgres;

--
-- TOC entry 5255 (class 0 OID 0)
-- Dependencies: 330
-- Name: contact_blacklist_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.contact_blacklist_id_seq OWNED BY nju_market.contact_blacklist.id;


--
-- TOC entry 332 (class 1259 OID 21783)
-- Name: conversations; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.conversations (
    conversation_id character varying(255) NOT NULL,
    last_message_content text,
    last_message_time timestamp with time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    user_id_1 character varying(50) NOT NULL,
    user_id_2 character varying(50) NOT NULL,
    user_1_count integer DEFAULT 0 NOT NULL,
    user_2_count integer DEFAULT 0 NOT NULL,
    user_1_visibility boolean NOT NULL,
    user_2_visibility boolean NOT NULL,
    user_1_last_message_content text,
    user_1_last_message_time timestamp(6) with time zone,
    user_2_last_message_content text,
    user_2_last_message_time timestamp(6) with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.conversations OWNER TO postgres;

--
-- TOC entry 5256 (class 0 OID 0)
-- Dependencies: 332
-- Name: TABLE conversations; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.conversations IS '对话表';


--
-- TOC entry 5257 (class 0 OID 0)
-- Dependencies: 332
-- Name: COLUMN conversations.conversation_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.conversation_id IS '对话ID';


--
-- TOC entry 5258 (class 0 OID 0)
-- Dependencies: 332
-- Name: COLUMN conversations.last_message_content; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.last_message_content IS '最后一条消息内容';


--
-- TOC entry 5259 (class 0 OID 0)
-- Dependencies: 332
-- Name: COLUMN conversations.last_message_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.last_message_time IS '最后消息时间';


--
-- TOC entry 5260 (class 0 OID 0)
-- Dependencies: 332
-- Name: COLUMN conversations.status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.status IS '对话状态：ACTIVE-活跃，ARCHIVED-已归档，DELETED-已删除';


--
-- TOC entry 5261 (class 0 OID 0)
-- Dependencies: 332
-- Name: COLUMN conversations.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.created_at IS '创建时间';


--
-- TOC entry 5262 (class 0 OID 0)
-- Dependencies: 332
-- Name: COLUMN conversations.updated_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.updated_at IS '更新时间';


--
-- TOC entry 334 (class 1259 OID 21801)
-- Name: image_references; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.image_references (
    image_id bigint NOT NULL,
    deleted_time timestamp(6) with time zone,
    file_size bigint,
    image_path character varying(500) NOT NULL,
    image_type character varying(20) NOT NULL,
    is_deleted boolean,
    last_reference_time timestamp(6) with time zone,
    reference_count integer NOT NULL,
    upload_time timestamp(6) with time zone,
    upload_user_id character varying(50)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.image_references OWNER TO postgres;

--
-- TOC entry 333 (class 1259 OID 21800)
-- Name: image_references_image_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.image_references_image_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.image_references_image_id_seq OWNER TO postgres;

--
-- TOC entry 5263 (class 0 OID 0)
-- Dependencies: 333
-- Name: image_references_image_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.image_references_image_id_seq OWNED BY nju_market.image_references.image_id;


--
-- TOC entry 335 (class 1259 OID 21815)
-- Name: messages; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.messages (
    message_id character varying(255) NOT NULL,
    conversation_id character varying(255) NOT NULL,
    sender_id character varying(50) NOT NULL,
    receiver_id character varying(50) NOT NULL,
    message_type character varying(20) DEFAULT 'TEXT'::character varying,
    content text NOT NULL,
    image_url character varying(500),
    is_read boolean DEFAULT false,
    read_time timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    deleted_by_receiver boolean,
    deleted_by_sender boolean,
    commodity_id character varying(50),
    order_id character varying(50)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.messages OWNER TO postgres;

--
-- TOC entry 5264 (class 0 OID 0)
-- Dependencies: 335
-- Name: TABLE messages; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.messages IS '消息表';


--
-- TOC entry 5265 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.message_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.message_id IS '消息ID';


--
-- TOC entry 5266 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.conversation_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.conversation_id IS '对话ID';


--
-- TOC entry 5267 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.sender_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.sender_id IS '发送者ID';


--
-- TOC entry 5268 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.receiver_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.receiver_id IS '接收者ID';


--
-- TOC entry 5269 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.message_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.message_type IS '消息类型：TEXT-文本，IMAGE-图片，COMMODITY-商品卡片，ORDER-订单卡片';


--
-- TOC entry 5270 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.content; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.content IS '消息内容';


--
-- TOC entry 5271 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.image_url; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.image_url IS '图片URL（当消息类型为IMAGE时）';


--
-- TOC entry 5272 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.is_read; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.is_read IS '是否已读';


--
-- TOC entry 5273 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.read_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.read_time IS '已读时间';


--
-- TOC entry 5274 (class 0 OID 0)
-- Dependencies: 335
-- Name: COLUMN messages.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.created_at IS '发送时间';


--
-- TOC entry 336 (class 1259 OID 21833)
-- Name: orders; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.orders (
    order_id character varying(50) NOT NULL,
    buyer_id character varying(50) NOT NULL,
    seller_id character varying(50) NOT NULL,
    commodity_id character varying(50) NOT NULL,
    order_status character varying(20) DEFAULT 'CREATED'::character varying NOT NULL,
    seller_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    buyer_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    pay_amount double precision NOT NULL,
    create_time timestamp with time zone,
    shipping_time timestamp with time zone,
    delivery_time timestamp with time zone,
    tracking_number character varying(100),
    shipping_address text,
    remark text,
    return_reason text,
    return_request_time timestamp with time zone,
    return_approval_time timestamp with time zone,
    return_rejection_reason text,
    return_tracking_number character varying(100),
    return_completion_time timestamp with time zone,
    quantity integer DEFAULT 1 NOT NULL,
    commodity_snapshot_title character varying(200),
    commodity_snapshot_description text,
    commodity_snapshot_price double precision,
    commodity_snapshot_location character varying(200),
    commodity_snapshot_category character varying(50),
    commodity_snapshot_condition_level character varying(20),
    commodity_snapshot_images text,
    commodity_snapshot_status character varying(20),
    commodity_snapshot_seller_name character varying(100),
    commodity_snapshot_seller_phone character varying(20),
    commodity_snapshot_seller_email character varying(100),
    commodity_snapshot_time timestamp with time zone,
    pay_time timestamp(6) with time zone,
    shipping_address_id character varying(50),
    shipping_address_snapshot_province character varying(50),
    shipping_address_snapshot_city character varying(50),
    shipping_address_snapshot_district character varying(50),
    shipping_address_snapshot_street character varying(200),
    shipping_address_snapshot_detail character varying(500),
    shipping_address_snapshot_full text,
    shipping_address_snapshot_recipient_name character varying(100),
    shipping_address_snapshot_recipient_phone character varying(20),
    commodity_snapshot_address_province character varying(50),
    commodity_snapshot_address_city character varying(50),
    commodity_snapshot_address_district character varying(50),
    commodity_snapshot_address_street character varying(200),
    commodity_snapshot_address_detail character varying(500),
    commodity_snapshot_address_full text
)
WITH (fillfactor='90');


ALTER TABLE nju_market.orders OWNER TO postgres;

--
-- TOC entry 5275 (class 0 OID 0)
-- Dependencies: 336
-- Name: TABLE orders; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.orders IS '订单表-包含商品快照信息';


--
-- TOC entry 5276 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.order_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.order_id IS '订单ID';


--
-- TOC entry 5277 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.buyer_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.buyer_id IS '买家用户ID';


--
-- TOC entry 5278 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.seller_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.seller_id IS '卖家用户ID';


--
-- TOC entry 5279 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_id IS '商品ID';


--
-- TOC entry 5280 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.order_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.order_status IS '订单状态: CREATED-已创建, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消, REFUNDED-已退款';


--
-- TOC entry 5281 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.seller_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.seller_visibility IS '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- TOC entry 5282 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.buyer_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.buyer_visibility IS '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- TOC entry 5283 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.create_time IS '创建时间';


--
-- TOC entry 5284 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_time IS '发货时间';


--
-- TOC entry 5285 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.delivery_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.delivery_time IS '签收时间';


--
-- TOC entry 5286 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.tracking_number; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.tracking_number IS '快递单号';


--
-- TOC entry 5287 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address IS '收货地址';


--
-- TOC entry 5288 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.remark; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.remark IS '订单备注';


--
-- TOC entry 5289 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.return_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_reason IS '退货原因';


--
-- TOC entry 5290 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.return_request_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_request_time IS '退货申请时间';


--
-- TOC entry 5291 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.return_approval_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_approval_time IS '退货审批时间';


--
-- TOC entry 5292 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.return_rejection_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_rejection_reason IS '退货拒绝原因';


--
-- TOC entry 5293 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.return_tracking_number; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_tracking_number IS '退货快递单号';


--
-- TOC entry 5294 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.return_completion_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_completion_time IS '退货完成时间';


--
-- TOC entry 5295 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.quantity; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.quantity IS '购买数量';


--
-- TOC entry 5296 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_title; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_title IS '商品快照-标题';


--
-- TOC entry 5297 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_description IS '商品快照-描述';


--
-- TOC entry 5298 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_location; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_location IS '商品快照-位置';


--
-- TOC entry 5299 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_category; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_category IS '商品快照-分类';


--
-- TOC entry 5300 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_condition_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_condition_level IS '商品快照-成色';


--
-- TOC entry 5301 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_images; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_images IS '商品快照-图片(JSON格式)';


--
-- TOC entry 5302 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_status IS '商品快照-状态';


--
-- TOC entry 5303 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_seller_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_seller_name IS '商品快照-卖家名称';


--
-- TOC entry 5304 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_seller_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_seller_phone IS '商品快照-卖家电话';


--
-- TOC entry 5305 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_seller_email; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_seller_email IS '商品快照-卖家邮箱';


--
-- TOC entry 5306 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_time IS '商品快照时间';


--
-- TOC entry 5307 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_id IS '收货地址ID（引用user_addresses表）';


--
-- TOC entry 5308 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_province IS '收货地址快照-省份';


--
-- TOC entry 5309 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_city IS '收货地址快照-城市';


--
-- TOC entry 5310 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_district IS '收货地址快照-区/县';


--
-- TOC entry 5311 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_street; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_street IS '收货地址快照-街道';


--
-- TOC entry 5312 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_detail; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_detail IS '收货地址快照-详细地址';


--
-- TOC entry 5313 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_full; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_full IS '收货地址快照-完整地址';


--
-- TOC entry 5314 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_recipient_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_recipient_name IS '收货地址快照-收货人姓名';


--
-- TOC entry 5315 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.shipping_address_snapshot_recipient_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_recipient_phone IS '收货地址快照-收货人电话';


--
-- TOC entry 5316 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_address_province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_province IS '商品地址快照-省份';


--
-- TOC entry 5317 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_address_city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_city IS '商品地址快照-城市';


--
-- TOC entry 5318 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_address_district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_district IS '商品地址快照-区/县';


--
-- TOC entry 5319 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_address_street; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_street IS '商品地址快照-街道';


--
-- TOC entry 5320 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_address_detail; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_detail IS '商品地址快照-详细地址';


--
-- TOC entry 5321 (class 0 OID 0)
-- Dependencies: 336
-- Name: COLUMN orders.commodity_snapshot_address_full; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_full IS '商品地址快照-完整地址';


--
-- TOC entry 337 (class 1259 OID 21849)
-- Name: return_reason_types; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.return_reason_types (
    reason_id character varying(20) NOT NULL,
    reason_name character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.return_reason_types OWNER TO postgres;

--
-- TOC entry 5322 (class 0 OID 0)
-- Dependencies: 337
-- Name: TABLE return_reason_types; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.return_reason_types IS '退货原因类型表';


--
-- TOC entry 5323 (class 0 OID 0)
-- Dependencies: 337
-- Name: COLUMN return_reason_types.reason_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.reason_id IS '原因ID';


--
-- TOC entry 5324 (class 0 OID 0)
-- Dependencies: 337
-- Name: COLUMN return_reason_types.reason_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.reason_name IS '原因名称';


--
-- TOC entry 5325 (class 0 OID 0)
-- Dependencies: 337
-- Name: COLUMN return_reason_types.description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.description IS '描述';


--
-- TOC entry 5326 (class 0 OID 0)
-- Dependencies: 337
-- Name: COLUMN return_reason_types.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.is_active IS '是否启用';


--
-- TOC entry 338 (class 1259 OID 21855)
-- Name: return_records; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.return_records (
    return_id character varying(50) NOT NULL,
    order_id character varying(50) NOT NULL,
    return_reason text,
    return_request_time timestamp with time zone,
    return_approval_time timestamp with time zone,
    return_status character varying(20) NOT NULL,
    return_rejection_reason text,
    return_tracking_number character varying(100),
    return_completion_time timestamp with time zone,
    created_time timestamp with time zone,
    updated_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.return_records OWNER TO postgres;

--
-- TOC entry 5327 (class 0 OID 0)
-- Dependencies: 338
-- Name: TABLE return_records; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.return_records IS '退货记录表';


--
-- TOC entry 5328 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_id IS '退货记录ID';


--
-- TOC entry 5329 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.order_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.order_id IS '订单ID';


--
-- TOC entry 5330 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_reason IS '退货原因';


--
-- TOC entry 5331 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_request_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_request_time IS '退货申请时间';


--
-- TOC entry 5332 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_approval_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_approval_time IS '退货审批时间';


--
-- TOC entry 5333 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_status IS '退货状态';


--
-- TOC entry 5334 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_rejection_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_rejection_reason IS '退货拒绝原因';


--
-- TOC entry 5335 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_tracking_number; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_tracking_number IS '退货快递单号';


--
-- TOC entry 5336 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.return_completion_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_completion_time IS '退货完成时间';


--
-- TOC entry 5337 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.created_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.created_time IS '创建时间';


--
-- TOC entry 5338 (class 0 OID 0)
-- Dependencies: 338
-- Name: COLUMN return_records.updated_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.updated_time IS '更新时间';


--
-- TOC entry 342 (class 1259 OID 22148)
-- Name: user_addresses; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.user_addresses (
    address_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    recipient_name character varying(100) NOT NULL,
    recipient_phone character varying(20) NOT NULL,
    province character varying(50) NOT NULL,
    city character varying(50) NOT NULL,
    district character varying(50) NOT NULL,
    street_address character varying(200) NOT NULL,
    detail_address character varying(500),
    full_address text NOT NULL,
    location public.geography(Point,4326),
    longitude double precision,
    latitude double precision,
    address_label character varying(20) DEFAULT 'HOME'::character varying,
    is_default boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE nju_market.user_addresses OWNER TO postgres;

--
-- TOC entry 5339 (class 0 OID 0)
-- Dependencies: 342
-- Name: TABLE user_addresses; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.user_addresses IS '用户地址表 - 存储用户的收货地址信息';


--
-- TOC entry 5340 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.address_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.address_id IS '地址ID';


--
-- TOC entry 5341 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.user_id IS '用户ID';


--
-- TOC entry 5342 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.recipient_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.recipient_name IS '收货人姓名';


--
-- TOC entry 5343 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.recipient_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.recipient_phone IS '收货人电话';


--
-- TOC entry 5344 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.province IS '省份';


--
-- TOC entry 5345 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.city IS '城市';


--
-- TOC entry 5346 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.district IS '区/县';


--
-- TOC entry 5347 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.street_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.street_address IS '街道地址';


--
-- TOC entry 5348 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.detail_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.detail_address IS '详细地址（楼栋、门牌号等）';


--
-- TOC entry 5349 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.full_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.full_address IS '完整地址（拼接后的完整地址）';


--
-- TOC entry 5350 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.location; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.location IS '地理位置（PostGIS Geography类型）';


--
-- TOC entry 5351 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.longitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.longitude IS '经度';


--
-- TOC entry 5352 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.latitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.latitude IS '纬度';


--
-- TOC entry 5353 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.address_label; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.address_label IS '地址标签: HOME-家, SCHOOL-学校, COMPANY-公司, OTHER-其他';


--
-- TOC entry 5354 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.is_default; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.is_default IS '是否默认地址';


--
-- TOC entry 5355 (class 0 OID 0)
-- Dependencies: 342
-- Name: COLUMN user_addresses.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.is_active IS '是否启用';


--
-- TOC entry 339 (class 1259 OID 21865)
-- Name: user_profiles; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.user_profiles (
    profile_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    nickname character varying(50),
    avatar character varying(500),
    buyer_order_has_new boolean,
    seller_order_has_new boolean
)
WITH (fillfactor='90');


ALTER TABLE nju_market.user_profiles OWNER TO postgres;

--
-- TOC entry 5356 (class 0 OID 0)
-- Dependencies: 339
-- Name: TABLE user_profiles; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.user_profiles IS '用户档案表';


--
-- TOC entry 5357 (class 0 OID 0)
-- Dependencies: 339
-- Name: COLUMN user_profiles.profile_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.profile_id IS '档案ID';


--
-- TOC entry 5358 (class 0 OID 0)
-- Dependencies: 339
-- Name: COLUMN user_profiles.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.user_id IS '用户ID';


--
-- TOC entry 5359 (class 0 OID 0)
-- Dependencies: 339
-- Name: COLUMN user_profiles.nickname; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.nickname IS '昵称';


--
-- TOC entry 5360 (class 0 OID 0)
-- Dependencies: 339
-- Name: COLUMN user_profiles.avatar; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.avatar IS '头像URL';


--
-- TOC entry 340 (class 1259 OID 21874)
-- Name: users; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.users (
    user_id character varying(50) NOT NULL,
    primary_phone character varying(20) NOT NULL,
    username character varying(50),
    password character varying(255),
    register_time timestamp with time zone,
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.users OWNER TO postgres;

--
-- TOC entry 5361 (class 0 OID 0)
-- Dependencies: 340
-- Name: TABLE users; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.users IS '用户表';


--
-- TOC entry 5362 (class 0 OID 0)
-- Dependencies: 340
-- Name: COLUMN users.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.user_id IS '用户ID';


--
-- TOC entry 5363 (class 0 OID 0)
-- Dependencies: 340
-- Name: COLUMN users.primary_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.primary_phone IS '主要手机号';


--
-- TOC entry 5364 (class 0 OID 0)
-- Dependencies: 340
-- Name: COLUMN users.username; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.username IS '用户名(可选)';


--
-- TOC entry 5365 (class 0 OID 0)
-- Dependencies: 340
-- Name: COLUMN users.password; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.password IS '用户密码(加密存储)';


--
-- TOC entry 5366 (class 0 OID 0)
-- Dependencies: 340
-- Name: COLUMN users.register_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.register_time IS '注册时间';


--
-- TOC entry 5367 (class 0 OID 0)
-- Dependencies: 340
-- Name: COLUMN users.account_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.account_status IS '账户状态: ACTIVE-活跃, SUSPENDED-暂停, BANNED-封禁';


--
-- TOC entry 341 (class 1259 OID 21878)
-- Name: visibility_types; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.visibility_types (
    type_id character varying(20) NOT NULL,
    type_name character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.visibility_types OWNER TO postgres;

--
-- TOC entry 5368 (class 0 OID 0)
-- Dependencies: 341
-- Name: TABLE visibility_types; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.visibility_types IS '可见性类型表';


--
-- TOC entry 5369 (class 0 OID 0)
-- Dependencies: 341
-- Name: COLUMN visibility_types.type_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.type_id IS '类型ID';


--
-- TOC entry 5370 (class 0 OID 0)
-- Dependencies: 341
-- Name: COLUMN visibility_types.type_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.type_name IS '类型名称';


--
-- TOC entry 5371 (class 0 OID 0)
-- Dependencies: 341
-- Name: COLUMN visibility_types.description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.description IS '描述';


--
-- TOC entry 5372 (class 0 OID 0)
-- Dependencies: 341
-- Name: COLUMN visibility_types.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.is_active IS '是否启用';


--
-- TOC entry 4879 (class 2604 OID 21776)
-- Name: contact_blacklist id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.contact_blacklist ALTER COLUMN id SET DEFAULT nextval('nju_market.contact_blacklist_id_seq'::regclass);


--
-- TOC entry 4886 (class 2604 OID 21804)
-- Name: image_references image_id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.image_references ALTER COLUMN image_id SET DEFAULT nextval('nju_market.image_references_image_id_seq'::regclass);


--
-- TOC entry 4914 (class 2606 OID 22030)
-- Name: admins idx_21724_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.admins
    ADD CONSTRAINT idx_21724_primary PRIMARY KEY (admin_id);


--
-- TOC entry 4929 (class 2606 OID 22033)
-- Name: commodities idx_21745_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodities
    ADD CONSTRAINT idx_21745_primary PRIMARY KEY (commodity_id);


--
-- TOC entry 4937 (class 2606 OID 22031)
-- Name: commodity_categories idx_21756_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_categories
    ADD CONSTRAINT idx_21756_primary PRIMARY KEY (category_id);


--
-- TOC entry 4939 (class 2606 OID 22039)
-- Name: commodity_snapshots idx_21761_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_snapshots
    ADD CONSTRAINT idx_21761_primary PRIMARY KEY (snapshot_id);


--
-- TOC entry 4943 (class 2606 OID 22043)
-- Name: contact_blacklist idx_21773_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.contact_blacklist
    ADD CONSTRAINT idx_21773_primary PRIMARY KEY (id);


--
-- TOC entry 4949 (class 2606 OID 22035)
-- Name: conversations idx_21783_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.conversations
    ADD CONSTRAINT idx_21783_primary PRIMARY KEY (conversation_id);


--
-- TOC entry 4952 (class 2606 OID 22042)
-- Name: image_references idx_21801_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.image_references
    ADD CONSTRAINT idx_21801_primary PRIMARY KEY (image_id);


--
-- TOC entry 4965 (class 2606 OID 22028)
-- Name: messages idx_21815_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.messages
    ADD CONSTRAINT idx_21815_primary PRIMARY KEY (message_id);


--
-- TOC entry 4983 (class 2606 OID 22029)
-- Name: orders idx_21833_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT idx_21833_primary PRIMARY KEY (order_id);


--
-- TOC entry 4986 (class 2606 OID 22032)
-- Name: return_reason_types idx_21849_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.return_reason_types
    ADD CONSTRAINT idx_21849_primary PRIMARY KEY (reason_id);


--
-- TOC entry 4990 (class 2606 OID 22051)
-- Name: return_records idx_21855_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.return_records
    ADD CONSTRAINT idx_21855_primary PRIMARY KEY (return_id);


--
-- TOC entry 4994 (class 2606 OID 22026)
-- Name: user_profiles idx_21865_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profiles
    ADD CONSTRAINT idx_21865_primary PRIMARY KEY (profile_id);


--
-- TOC entry 4999 (class 2606 OID 22027)
-- Name: users idx_21874_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.users
    ADD CONSTRAINT idx_21874_primary PRIMARY KEY (user_id);


--
-- TOC entry 5003 (class 2606 OID 22037)
-- Name: visibility_types idx_21878_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.visibility_types
    ADD CONSTRAINT idx_21878_primary PRIMARY KEY (type_id);


--
-- TOC entry 5009 (class 2606 OID 22159)
-- Name: user_addresses user_addresses_pkey; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_addresses
    ADD CONSTRAINT user_addresses_pkey PRIMARY KEY (address_id);


--
-- TOC entry 4907 (class 1259 OID 21924)
-- Name: idx_21724_idx_account_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_account_status ON nju_market.admins USING btree (account_status);


--
-- TOC entry 4908 (class 1259 OID 21922)
-- Name: idx_21724_idx_admin_level; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_admin_level ON nju_market.admins USING btree (admin_level);


--
-- TOC entry 4909 (class 1259 OID 21923)
-- Name: idx_21724_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_create_time ON nju_market.admins USING btree (create_time);


--
-- TOC entry 4910 (class 1259 OID 21925)
-- Name: idx_21724_idx_department; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_department ON nju_market.admins USING btree (department);


--
-- TOC entry 4911 (class 1259 OID 21920)
-- Name: idx_21724_idx_last_login_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_last_login_time ON nju_market.admins USING btree (last_login_time);


--
-- TOC entry 4912 (class 1259 OID 21926)
-- Name: idx_21724_idx_username; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_username ON nju_market.admins USING btree (username);


--
-- TOC entry 4915 (class 1259 OID 21949)
-- Name: idx_21724_username; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21724_username ON nju_market.admins USING btree (username);


--
-- TOC entry 4916 (class 1259 OID 21930)
-- Name: idx_21745_idx_category; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_category ON nju_market.commodities USING btree (category);


--
-- TOC entry 4917 (class 1259 OID 21935)
-- Name: idx_21745_idx_click_count; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_click_count ON nju_market.commodities USING btree (click_count);


--
-- TOC entry 4918 (class 1259 OID 21929)
-- Name: idx_21745_idx_condition_level; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_condition_level ON nju_market.commodities USING btree (condition_level);


--
-- TOC entry 4919 (class 1259 OID 21936)
-- Name: idx_21745_idx_price; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_price ON nju_market.commodities USING btree (price);


--
-- TOC entry 4920 (class 1259 OID 21950)
-- Name: idx_21745_idx_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_publish_time ON nju_market.commodities USING btree (publish_time);


--
-- TOC entry 4921 (class 1259 OID 21942)
-- Name: idx_21745_idx_seller_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_seller_id ON nju_market.commodities USING btree (seller_id);


--
-- TOC entry 4922 (class 1259 OID 21939)
-- Name: idx_21745_idx_seller_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_seller_publish_time ON nju_market.commodities USING btree (seller_id, publish_time);


--
-- TOC entry 4923 (class 1259 OID 21937)
-- Name: idx_21745_idx_seller_status_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_seller_status_publish_time ON nju_market.commodities USING btree (seller_id, commodity_status, publish_time);


--
-- TOC entry 4924 (class 1259 OID 21945)
-- Name: idx_21745_idx_status_click_count; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_click_count ON nju_market.commodities USING btree (commodity_status, click_count);


--
-- TOC entry 4925 (class 1259 OID 21941)
-- Name: idx_21745_idx_status_price; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_price ON nju_market.commodities USING btree (commodity_status, price);


--
-- TOC entry 4926 (class 1259 OID 21973)
-- Name: idx_21745_idx_status_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_publish_time ON nju_market.commodities USING btree (commodity_status, publish_time);


--
-- TOC entry 4927 (class 1259 OID 21947)
-- Name: idx_21745_idx_stock; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_stock ON nju_market.commodities USING btree (stock);


--
-- TOC entry 4933 (class 1259 OID 21943)
-- Name: idx_21756_idx_is_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21756_idx_is_active ON nju_market.commodity_categories USING btree (is_active);


--
-- TOC entry 4934 (class 1259 OID 21927)
-- Name: idx_21756_idx_parent_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21756_idx_parent_id ON nju_market.commodity_categories USING btree (parent_id);


--
-- TOC entry 4935 (class 1259 OID 21958)
-- Name: idx_21756_idx_sort_order; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21756_idx_sort_order ON nju_market.commodity_categories USING btree (sort_order);


--
-- TOC entry 4940 (class 1259 OID 21987)
-- Name: idx_21773_idx_blocked_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21773_idx_blocked_user_id ON nju_market.contact_blacklist USING btree (blocked_user_id);


--
-- TOC entry 4941 (class 1259 OID 21970)
-- Name: idx_21773_idx_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21773_idx_user_id ON nju_market.contact_blacklist USING btree (user_id);


--
-- TOC entry 4944 (class 1259 OID 21968)
-- Name: idx_21773_uk_user_blocked; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21773_uk_user_blocked ON nju_market.contact_blacklist USING btree (user_id, blocked_user_id);


--
-- TOC entry 4945 (class 1259 OID 21953)
-- Name: idx_21783_idx_last_message_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21783_idx_last_message_time ON nju_market.conversations USING btree (last_message_time);


--
-- TOC entry 4946 (class 1259 OID 21944)
-- Name: idx_21783_idx_user1_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21783_idx_user1_status_visibility_time ON nju_market.conversations USING btree (user_id_1, status, user_1_visibility, last_message_time);


--
-- TOC entry 4947 (class 1259 OID 21952)
-- Name: idx_21783_idx_user2_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21783_idx_user2_status_visibility_time ON nju_market.conversations USING btree (user_id_2, status, user_2_visibility, last_message_time);


--
-- TOC entry 4950 (class 1259 OID 21954)
-- Name: idx_21783_uk_user_pair_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21783_uk_user_pair_active ON nju_market.conversations USING btree (user_id_1, user_id_2, status);


--
-- TOC entry 4953 (class 1259 OID 22007)
-- Name: idx_21801_uk_ie6gy369soc8701jlxwe85tms; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21801_uk_ie6gy369soc8701jlxwe85tms ON nju_market.image_references USING btree (image_path);


--
-- TOC entry 4954 (class 1259 OID 21892)
-- Name: idx_21815_idx_conversation_deleted_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_deleted_time ON nju_market.messages USING btree (conversation_id, deleted_by_sender, deleted_by_receiver, created_at);


--
-- TOC entry 4955 (class 1259 OID 21895)
-- Name: idx_21815_idx_conversation_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_id ON nju_market.messages USING btree (conversation_id);


--
-- TOC entry 4956 (class 1259 OID 21896)
-- Name: idx_21815_idx_conversation_receiver_read_deleted; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_receiver_read_deleted ON nju_market.messages USING btree (conversation_id, receiver_id, is_read, deleted_by_sender, deleted_by_receiver);


--
-- TOC entry 4957 (class 1259 OID 21899)
-- Name: idx_21815_idx_conversation_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_time ON nju_market.messages USING btree (conversation_id, created_at);


--
-- TOC entry 4958 (class 1259 OID 21914)
-- Name: idx_21815_idx_created_at; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_created_at ON nju_market.messages USING btree (created_at);


--
-- TOC entry 4959 (class 1259 OID 21902)
-- Name: idx_21815_idx_is_read; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_is_read ON nju_market.messages USING btree (is_read);


--
-- TOC entry 4960 (class 1259 OID 21911)
-- Name: idx_21815_idx_messages_commodity_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_messages_commodity_id ON nju_market.messages USING btree (commodity_id);


--
-- TOC entry 4961 (class 1259 OID 21903)
-- Name: idx_21815_idx_messages_order_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_messages_order_id ON nju_market.messages USING btree (order_id);


--
-- TOC entry 4962 (class 1259 OID 21898)
-- Name: idx_21815_idx_receiver_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_receiver_time ON nju_market.messages USING btree (receiver_id, created_at);


--
-- TOC entry 4963 (class 1259 OID 21901)
-- Name: idx_21815_idx_sender_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_sender_time ON nju_market.messages USING btree (sender_id, created_at);


--
-- TOC entry 4966 (class 1259 OID 21906)
-- Name: idx_21833_idx_buyer_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_id ON nju_market.orders USING btree (buyer_id);


--
-- TOC entry 4967 (class 1259 OID 21913)
-- Name: idx_21833_idx_buyer_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_status_visibility_time ON nju_market.orders USING btree (buyer_id, order_status, buyer_visibility, create_time);


--
-- TOC entry 4968 (class 1259 OID 21904)
-- Name: idx_21833_idx_buyer_visibility; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_visibility ON nju_market.orders USING btree (buyer_visibility);


--
-- TOC entry 4969 (class 1259 OID 21907)
-- Name: idx_21833_idx_buyer_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_visibility_time ON nju_market.orders USING btree (buyer_id, buyer_visibility, create_time);


--
-- TOC entry 4970 (class 1259 OID 21910)
-- Name: idx_21833_idx_commodity_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_commodity_id ON nju_market.orders USING btree (commodity_id);


--
-- TOC entry 4971 (class 1259 OID 21905)
-- Name: idx_21833_idx_commodity_snapshot_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_commodity_snapshot_time ON nju_market.orders USING btree (commodity_snapshot_time);


--
-- TOC entry 4972 (class 1259 OID 21912)
-- Name: idx_21833_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_create_time ON nju_market.orders USING btree (create_time);


--
-- TOC entry 4973 (class 1259 OID 21932)
-- Name: idx_21833_idx_delivery_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_delivery_time ON nju_market.orders USING btree (delivery_time);


--
-- TOC entry 4974 (class 1259 OID 21933)
-- Name: idx_21833_idx_order_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_order_status ON nju_market.orders USING btree (order_status);


--
-- TOC entry 4975 (class 1259 OID 21916)
-- Name: idx_21833_idx_pay_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_pay_time ON nju_market.orders USING btree (pay_time);


--
-- TOC entry 4976 (class 1259 OID 21938)
-- Name: idx_21833_idx_return_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_return_status ON nju_market.orders USING btree (order_status);


--
-- TOC entry 4977 (class 1259 OID 21917)
-- Name: idx_21833_idx_seller_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_id ON nju_market.orders USING btree (seller_id);


--
-- TOC entry 4978 (class 1259 OID 21919)
-- Name: idx_21833_idx_seller_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_status_visibility_time ON nju_market.orders USING btree (seller_id, order_status, seller_visibility, create_time);


--
-- TOC entry 4979 (class 1259 OID 21918)
-- Name: idx_21833_idx_seller_visibility; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_visibility ON nju_market.orders USING btree (seller_visibility);


--
-- TOC entry 4980 (class 1259 OID 21908)
-- Name: idx_21833_idx_seller_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_visibility_time ON nju_market.orders USING btree (seller_id, seller_visibility, create_time);


--
-- TOC entry 4981 (class 1259 OID 21909)
-- Name: idx_21833_idx_shipping_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_shipping_time ON nju_market.orders USING btree (shipping_time);


--
-- TOC entry 4987 (class 1259 OID 22010)
-- Name: idx_21855_idx_order_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21855_idx_order_id ON nju_market.return_records USING btree (order_id);


--
-- TOC entry 4988 (class 1259 OID 22006)
-- Name: idx_21855_idx_return_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21855_idx_return_status ON nju_market.return_records USING btree (return_status);


--
-- TOC entry 4991 (class 1259 OID 21886)
-- Name: idx_21865_idx_user_profile_nickname; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21865_idx_user_profile_nickname ON nju_market.user_profiles USING btree (nickname);


--
-- TOC entry 4992 (class 1259 OID 21885)
-- Name: idx_21865_idx_user_profile_nickname_avatar; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21865_idx_user_profile_nickname_avatar ON nju_market.user_profiles USING btree (user_id, nickname, avatar);


--
-- TOC entry 4995 (class 1259 OID 21894)
-- Name: idx_21865_uk_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21865_uk_user_id ON nju_market.user_profiles USING btree (user_id);


--
-- TOC entry 4996 (class 1259 OID 21897)
-- Name: idx_21874_idx_account_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21874_idx_account_status ON nju_market.users USING btree (account_status);


--
-- TOC entry 4997 (class 1259 OID 21887)
-- Name: idx_21874_idx_register_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21874_idx_register_time ON nju_market.users USING btree (register_time);


--
-- TOC entry 5000 (class 1259 OID 21893)
-- Name: idx_21874_uk_primary_phone; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21874_uk_primary_phone ON nju_market.users USING btree (primary_phone);


--
-- TOC entry 5001 (class 1259 OID 21891)
-- Name: idx_21874_uk_username; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21874_uk_username ON nju_market.users USING btree (username);


--
-- TOC entry 4930 (class 1259 OID 22175)
-- Name: idx_commodities_address_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_commodities_address_id ON nju_market.commodities USING btree (address_id);


--
-- TOC entry 4931 (class 1259 OID 22177)
-- Name: idx_commodities_city_district; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_commodities_city_district ON nju_market.commodities USING btree (address_snapshot_city, address_snapshot_district);


--
-- TOC entry 4932 (class 1259 OID 22176)
-- Name: idx_commodities_location_geography; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_commodities_location_geography ON nju_market.commodities USING gist (location_geography);


--
-- TOC entry 4984 (class 1259 OID 22169)
-- Name: idx_orders_shipping_address_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_orders_shipping_address_id ON nju_market.orders USING btree (shipping_address_id);


--
-- TOC entry 5004 (class 1259 OID 22168)
-- Name: idx_user_addresses_location; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_location ON nju_market.user_addresses USING gist (location);


--
-- TOC entry 5005 (class 1259 OID 22167)
-- Name: idx_user_addresses_user_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_user_active ON nju_market.user_addresses USING btree (user_id, is_active) WHERE (is_active = true);


--
-- TOC entry 5006 (class 1259 OID 22166)
-- Name: idx_user_addresses_user_default; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_user_default ON nju_market.user_addresses USING btree (user_id, is_default) WHERE (is_default = true);


--
-- TOC entry 5007 (class 1259 OID 22165)
-- Name: idx_user_addresses_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_user_id ON nju_market.user_addresses USING btree (user_id);


--
-- TOC entry 5035 (class 2620 OID 22184)
-- Name: user_addresses trigger_user_addresses_update_time; Type: TRIGGER; Schema: nju_market; Owner: postgres
--

CREATE TRIGGER trigger_user_addresses_update_time BEFORE UPDATE ON nju_market.user_addresses FOR EACH ROW EXECUTE FUNCTION nju_market.update_user_addresses_updated_time();


--
-- TOC entry 5023 (class 2606 OID 22178)
-- Name: commodities fk_commodities_address_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodities
    ADD CONSTRAINT fk_commodities_address_id FOREIGN KEY (address_id) REFERENCES nju_market.user_addresses(address_id) ON DELETE SET NULL;


--
-- TOC entry 5024 (class 2606 OID 22072)
-- Name: commodities fk_commodities_seller_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodities
    ADD CONSTRAINT fk_commodities_seller_id FOREIGN KEY (seller_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 5026 (class 2606 OID 22107)
-- Name: orders fk_orders_buyer_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_buyer_id FOREIGN KEY (buyer_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 5027 (class 2606 OID 22112)
-- Name: orders fk_orders_commodity_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_commodity_id FOREIGN KEY (commodity_id) REFERENCES nju_market.commodities(commodity_id) ON DELETE CASCADE;


--
-- TOC entry 5028 (class 2606 OID 22117)
-- Name: orders fk_orders_seller_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_seller_id FOREIGN KEY (seller_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 5029 (class 2606 OID 22170)
-- Name: orders fk_orders_shipping_address_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_shipping_address_id FOREIGN KEY (shipping_address_id) REFERENCES nju_market.user_addresses(address_id) ON DELETE SET NULL;


--
-- TOC entry 5030 (class 2606 OID 22127)
-- Name: return_records fk_return_order; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.return_records
    ADD CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES nju_market.orders(order_id) ON DELETE CASCADE;


--
-- TOC entry 5032 (class 2606 OID 22160)
-- Name: user_addresses fk_user_addresses_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_addresses
    ADD CONSTRAINT fk_user_addresses_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 5031 (class 2606 OID 22137)
-- Name: user_profiles fk_user_profiles_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profiles
    ADD CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 5025 (class 2606 OID 22097)
-- Name: messages messages_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.messages
    ADD CONSTRAINT messages_ibfk_1 FOREIGN KEY (conversation_id) REFERENCES nju_market.conversations(conversation_id) ON DELETE CASCADE;


-- Completed on 2026-03-10 11:59:02

--
-- PostgreSQL database dump complete
--

\unrestrict AKE2LVKKSiYr4NszPHsPFzia2eehc6TK2DBR54YMNHpogxPax09O8fAlJCC6Jru

