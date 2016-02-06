(ns todo.categories
  (:require [posh.core :as p]
            [todo.util :as util]
            [todo.tasks :as tasks]
            [todo.components :as comp]
            [todo.dashboard :as dash]))

;; todo components

(defn delete-category [conn category-id]
  (let [category @(p/pull conn [:category/name] category-id)]
    [comp/stage-button
     [(str "Delete \"" (:category/name category) "\" Category") "This will delete all its tasks, ok?"]
     #(p/transact! conn [[:db.fn/retractEntity category-id]])]))

(defn category-panel [conn todo-id]
  (let [c @(p/q conn '[:find ?c .
                       :in $ ?t
                       :where
                       [?t :todo/display-category ?c]]
                todo-id)]
    (if (not c)
      [dash/dashboard conn todo-id]
      [:div
       [:h2 [comp/editable-label conn c :category/name]]
       [delete-category conn c]
       [tasks/task-panel conn c]
       ;[add-task c]
       ])))

(defn add-category!
  [conn todo-id category-name]
  (util/new-entity! conn {:category/name category-name :category/todo todo-id}))

(defn add-new-category [conn todo-id]
  [:div "Add new category: " [comp/add-box conn (partial add-category! conn todo-id)]])

(defn category-item [conn todo-id category]
  [:button
   {:onClick #(p/transact!
               conn
               [[:db/add todo-id :todo/display-category (:db/id category)]])}
   (:category/name category)
   " (" (count (:task/_category category)) ")"])

(defn category-menu [conn todo-id]
  (let [cats (->> @(p/pull conn
                           '[{:category/_todo [:db/id :category/name {:task/_category [:db/id]}]}]
                           todo-id)
                  :category/_todo
                  (sort-by :category/name))]
    [:span
     (for [c cats]
       ^{:key (:db/id c)}
       [category-item conn todo-id c])]))
